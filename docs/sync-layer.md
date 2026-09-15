# Sync-шар Debt-Tracker

Документ зібрано з коментарів і KDoc у коді (`sharedUI`, `supabase/migrations`).
Джерела вказані біля кожного розділу.

---

## 1. Загальна модель: offline-first (спек §5)

Дві принципово різні реалізації залежно від платформи:

| Платформа            | `DebtorRepository` / `CreditorRepository` | Локальний кеш | Sync-механізм                     |
|----------------------|-------------------------------------------|---------------|-----------------------------------|
| Android / iOS / Desktop (JVM) | `RoomDebtorRepository` / `RoomCreditorRepository` | Room (SQLite) — джерело правди | `SyncCoordinator` (push + pull)   |
| Web (JS / wasmJS)    | `SupabaseDebtorRepository` / `SupabaseCreditorRepository` | немає (Room не має js/wasmJs таргета, спек §1) | online-only, прямий Postgrest + Realtime |

Прив'язка реалізацій — у `platformDataModule()` (`core/di/PlatformModule.<platform>.kt`);
спільні use cases поверх інтерфейсів репозиторіїв — у `appModule` (`core/di/AppModule.kt`).

### Room-платформи (offline-first)

- Усі write-операції йдуть у Room із `syncStatus = PENDING`
  (`RoomDebtorRepository`, `RoomCreditorRepository`).
- Фактичний push у Supabase виконує фоновий `SyncCoordinator` — читає `PENDING`-записи
  (спочатку планувалося як «Фаза 6 / background sync worker»).
- UI завжди читає з Room; мережа на критичному шляху відсутня.

### Web (online-only)

- Немає локального кешу — read/write ідуть напряму через Supabase Postgrest.
- Списки лишаються «живими» через Realtime (`selectAsFlow`) — це дзеркалить pull-бік
  `SyncCoordinator`.
- `clearLocalCache()` — no-op (нічого чистити при виході).
- `SyncStatusProvider` на Web — заглушка, яка завжди повертає `SyncUiStatus.Synced`
  (немає черги pending-записів, про яку звітувати).

---

## 2. `SyncCoordinator` (`data/sync/SyncCoordinator.kt`, sourceSet `roomMain`)

> Offline-first sync (спек §5): **push** — цикл раз на 30 с відправляє PENDING-рядки
> у Supabase; **pull** — реактивна Realtime-підписка (`selectAsFlow`) на всі 4 таблиці,
> відфільтрована по `user_id`. LWW за `updatedAt` застосовується тільки до
> Debtor/Creditor-метаданих; транзакції не мерджаться (просто upsert по `id`),
> оскільки конфлікт на рівні одного запису з різним `id` неможливий.
>
> Спрощення відносно оригінального опису (WorkManager на Android): єдиний
> coroutine-цикл на всіх платформах замість платформо-специфічного планувальника
> — функціонально еквівалентно для персонального застосунку, простіше й не
> потребує окремого expect/actual шару. Легко замінити на WorkManager пізніше,
> не чіпаючи домен/дані.

### Залежності конструктора

`SupabaseClient`, `AuthRepository`, `DebtorDao`, `DebtTransactionDao`, `CreditorDao`,
`CreditorTransactionDao`, `CoroutineScope` (`ApplicationScope`), `AppSettings`,
`Connectivity` ([`dev.jordond.connectivity`](https://github.com/jordond/connectivity),
`roomMain` — Web не має `SyncCoordinator` взагалі, тож і моніторингу немає).

Реалізує `SyncStatusProvider` (домен) — біндиться в DI як `single<SyncStatusProvider> { get<SyncCoordinator>() }`.

### Життєвий цикл

- `start()` запускається один раз на процес:
  - Android — `DebtTrackerApplication.onCreate()`
  - Desktop — `desktopApp/src/main/kotlin/main.kt`
  - iOS — `sharedUI/src/iosMain/kotlin/main.kt`
- `start()` підписується на `authRepository.isAuthenticated` через `collectLatest`:
  - авторизований → `runSyncSession()`
  - вийшов → `_status.value = SyncUiStatus.Synced`, сесія скасовується
- `collectLatest` сам зупиняє/перезапускає всю sync-сесію при вході/виході — окремого
  тіньдауну не потрібно.

### `runSyncSession()`

У межах одного `coroutineScope` запускає 6 паралельних корутин:

```
launch { resilient { pushLoop() } }
launch { resilient { pullDebtors(userId) } }
launch { resilient { pullDebtTransactions(userId) } }
launch { resilient { pullCreditors(userId) } }
launch { resilient { pullCreditorTransactions(userId) } }
launch { resilient { pushOnReconnect() } }
```

---

## 3. Push

### `pushLoop()`
Нескінченний цикл: `pushPending()` → `delay(30_000 ms)`.

### `pushPending()`
1. `authRepository.currentUserId ?: return` — без сесії не пушимо.
2. Збирає `getPending()` з усіх 4 DAO (`WHERE syncStatus = 'PENDING'`).
3. `totalPending == 0` → `_status = Synced`, вихід.
4. Інакше `_status = Syncing`.
5. Для кожного pending-рядка:
   - `client.from("<table>").upsert(entity.toDto(userId))`
   - при успіху — локально помічає `syncStatus = SYNCED`;
   - при помилці — `failures++` (рядок лишається `PENDING`, повториться наступного циклу).
6. Підсумок: `failures > 0 → SyncUiStatus.OfflinePending(failures)`, інакше `Synced`.

> **Важлива деталь про debtors/creditors при push:**
> рядки вже існують локально (прочитані з `getPending()`), тож для оновлення локального
> прапорця використовується `dao.update(...)`, а **не** `upsert()`. Причина — див. розділ 6.

### `refreshNow()` (pull-to-refresh hook, з `SyncStatusProvider`)
> Pushes any PENDING local writes immediately instead of waiting for the next cycle.

Викликається з `DebtorDetailViewModel` / `CreditorDetailViewModel` / `*ListViewModel` при
свайпі «потягнути щоб оновити». На Web — no-op.

### `pushOnReconnect()` — тригер по відновленню мережі

`pushLoop()` і так покриває стабільний стан (ретрай раз на 30 с), але при реальному розриві
мережі це до 30 с, коли вже відправлений би pending-рядок просто чекає своєї черги, хоча
з'єднання вже відновилось. `pushOnReconnect()` слухає `Connectivity.statusUpdates`
([`dev.jordond.connectivity`](https://github.com/jordond/connectivity)) і викликає
`pushPending()` одразу на переході `Disconnected → Connected`, а не на кожному `Connected`
(інакше перший emit при старті сесії дублював би push, який `pushLoop()` вже й так робить
одразу при вході в `runSyncSession()`).

```kotlin
private suspend fun pushOnReconnect() {
    var previous: Connectivity.Status? = null
    connectivity.statusUpdates.collect { current ->
        if (current is Connectivity.Status.Connected && previous is Connectivity.Status.Disconnected) {
            pushPending()
        }
        previous = current
    }
}
```

Обгорнутий у `resilient {}` як і pull-петлі — той самий SupervisorJob-без-handler ризик.

`Connectivity` — Koin `single`, платформо-специфічний завод у `platformDataModule()`:
- Android / iOS — `connectivity-device` (нативний `ConnectivityManager`/`NWPathMonitor`).
- Desktop (JVM) — `connectivity-http` (у `connectivity-device` немає JVM-таргета) —
  HTTP-опитування `google.com`/`github.com`/`bing.com`:443, `pollingIntervalMs = 15.seconds`
  замість дефолтних 5 хвилин, щоб не бути повільнішим за власний 30-секундний ретрай `pushLoop()`.

`autoStart = true` в усіх трьох — моніторинг живе весь процес, а не лише в межах
`runSyncSession()`/автентифікованої сесії.

---

## 4. Pull

Кожна pull-функція — реактивна підписка:

```kotlin
client.from("<table>")
    .selectAsFlow(<Dto>::id, filter = FilterOperation("user_id", FilterOperator.EQ, userId))
    .collectLatest { remoteRows -> ... }
```

### Debtor / Creditor (метадані) — LWW-мердж
Для кожного DTO:
- локального немає → `dao.upsert(dto.toEntity())` (новий рядок);
- локальний є → перезаписуємо через `dao.update(dto.toEntity())` **лише якщо**
  `local.syncStatus != PENDING || local.updatedAt <= remoteUpdatedAt`
  (тобто локальні не-відправлені зміни новіші за серверні — не затираємо їх).

### Транзакції — без мерджу
> Транзакції не мерджаться (спек §5) — прямий `upsert` по `id`.

Конфлікт на рівні одного запису з різним `id` неможливий, тому LWW не потрібен.

### Мапінг: `data/sync/RemoteMapper.kt`
> Room entity ↔ Supabase DTO mapping, used by `SyncCoordinator`. Local
> rows pulled from the server are always tagged `SyncStatus.SYNCED` — sync direction is
> one-way per call, never round-tripped through this mapper.

- `amount`: `numeric(14,2)` у Postgres → `Double` у DTO (так PostgREST серіалізує `numeric`).
  Конвертація в/з `BigDecimal` — у мапері; у межах `numeric(14,2)` `Double` не втрачає точності.
- `type` транзакції не зберігається на сервері — виводиться зі знаку `amount` у мапері
  (`toDebtTransactionType()` / `toCreditorTransactionType()`).

---

## 5. Стійкість pull-підписок: `resilient { }`

> Realtime pull-функції теоретично мають висіти вічно (доки їх не скасують), але
> supabase-kt має відомий race: якщо вебсокет розірветься саме між перевіркою статусу
> каналу та відправкою LEAVE-повідомлення в `unsubscribe()`, кидається
> `IllegalStateException("Websocket not yet initialized")`. `ApplicationScope` — це
> `SupervisorJob` без `CoroutineExceptionHandler`, тож без цієї обгортки будь-яка
> необроблена помилка тут (ця гонка, розрив мережі тощо) валить увесь застосунок.
> Перепідписка через новий канал — найпростіший спосіб відновитись.

```kotlin
private suspend fun resilient(block: suspend () -> Unit) {
    while (currentCoroutineContext().isActive) {
        try { block() }
        catch (e: CancellationException) { throw e }   // скасування пропускаємо далі
        catch (e: Exception) { delay(5_000.milliseconds) } // будь-що інше — пауза й перепідписка
    }
}
```

`ApplicationScope` (`core/di/ApplicationScope.kt`):
> Живе стільки ж, скільки застосунок — для `AuthRepository.isAuthenticated` і `SyncCoordinator`.
`CoroutineScope(SupervisorJob() + Dispatchers.Default)`.

---

## 6. Каскадне видалення транзакцій: чому `upsert()` небезпечний

Повторюваний коментар у `DebtorDao`, `RoomDebtorRepository`, `RoomCreditorRepository`,
`SyncCoordinator.pushPending()` та `SyncCoordinator.pullDebtors()`:

> `@Insert(onConflict = REPLACE)` (`INSERT OR REPLACE`) на існуючому PK у SQLite
> **видаляє-і-заново-вставляє** рядок. Це каскадно видалить усі транзакції цього
> боржника/кредитора через їхній `ON DELETE CASCADE` FK.

Тому скрізь діє правило:
- **новий** рядок → `dao.upsert(entity)`;
- **існуючий** рядок → `dao.update(entity)` (перед цим — перевірка `dao.getById(id) != null`).

Транзакції ж вставляються через `upsert()` вільно — у них немає дочірніх рядків.

`softDeleteDebtor` / `softDeleteCreditor` — це `update(entity.copy(isDeleted = true, syncStatus = PENDING, updatedAt = now()))`,
жодного фізичного `DELETE`. DAO-запити фільтрують `WHERE isDeleted = 0`.

---

## 7. Клієнтський мірор Postgres-логіки

Postgres має тригер `recalc_debtor_status` (Фаза 0), який рахує `status`/`updatedAt`
боржника з його транзакцій. Room-репозиторій дублює це локально:

> `recalcDebtorStatus(debtorId)` — мірор Postgres-тригера: `status`/`updatedAt`
> рахуються з транзакцій.

Викликається після кожного `addTransaction(...)`. На Web — `recalcDebtorStatus(debtorId, userId)`
робить те саме через окремі Postgrest-запити (бо тригерний результат не приходить назад
у той самий виклик синхронно).

---

## 8. `SyncUiStatus` та індикатор синхронізації

`domain/model/SyncUiStatus.kt`:
> Індикатор синхронізації (спек §5) — рендериться тільки для авторизованих користувачів.

```kotlin
sealed interface SyncUiStatus {
    data object Synced : SyncUiStatus
    data object Syncing : SyncUiStatus
    data class OfflinePending(val count: Int) : SyncUiStatus
}
```

`domain/sync/SyncStatusProvider.kt`:
> Абстракція над `SyncCoordinator` (`roomMain`, недоступний з `commonMain`) — щоб
> `HomeScreen` міг показати індикатор синхронізації (спек §5) без залежності від
> Room-типів на Web.

```kotlin
interface SyncStatusProvider {
    val status: StateFlow<SyncUiStatus>
    suspend fun refreshNow()   // pull-to-refresh hook
}
```

Споживачі `status`: `HomeViewModel`, `Debtor/CreditorListViewModel`, `Debtor/CreditorDetailViewModel`.
У прев'ю підставляється `FakeSyncStatusProvider` (`preview/PreviewFakes.kt`).

---

## 9. Дзеркалювання боргів між акаунтами (`supabase/migrations/0007_bidirectional_sync_and_notifications.sql`, `0012_harden_debt_mirroring_rls.sql`, `0013_phone_link_consent.sql`)

Це серверна логіка, не клієнтський sync, але вона протікає в клієнт через звичайний pull.

### Модель знаків сум
> `debt_transactions.amount` і `creditor_transactions.amount` використовують **однаковий**
> знак для того самого грошового руху: LEND (я дав, від'ємне) і BORROW (я взяв, від'ємне)
> — це одна й та сама подія, побачена власником боргу і боржником; так само REPAY/RETURN
> (додатні). Дзеркальний рядок копіює `amount` без інверсії знаку.

### Захист від рекурсії тригерів
> `mirror_transaction_id` заповнюється **лише** на авто-створеному дзеркальному рядку
> (вказує назад на рядок-джерело в іншій таблиці). Оригінальний рядок, уведений користувачем
> вручну, це поле не заповнює ніколи — це і є захист від нескінченної рекурсії тригерів.

`propagate_debt_transaction()` / `propagate_creditor_transaction()`: якщо
`NEW.mirror_transaction_id is not null` → `return NEW` одразу (рядок сам є дзеркалом).

### Прив'язка — email миттєво, телефон лише за згодою (0013)
- `link_debtor_to_registered_user(p_debtor_id)` / `link_creditor_to_registered_user(p_creditor_id)`
  — `SECURITY DEFINER` RPC. **Ідемпотентна** — повторний виклик для вже прив'язаного запису
  повертає наявний `mirror_*_id`.
  - **Email-матч** (Supabase Auth сам верифікує email) — дзеркалить одразу, як і раніше:
    створює `creditor`/`debtor` у акаунті цілі, копіює всі наявні транзакції, пише
    `DEBTOR_LINKED`/`CREDITOR_LINKED` у `notifications`. Сама логіка дзеркалювання винесена
    в приватні `perform_debtor_link`/`perform_creditor_link` (0013) — не виставлені через
    PostgREST, той самий підхід, що й `propagate_debt_transaction`.
  - **Phone-матч** (B3, нічим не верифікований — див. security-аудит) більше НЕ лінкує
    одразу: створює рядок у `pending_link_requests` (`status = 'pending'`, унікальний
    partial-індекс на `(kind, source_id) where status = 'pending'` — без дублікатів при
    повторному виклику) і сповіщає ціль типом `LINK_REQUEST`. Повертає `null`, доки ціль не
    вирішить.
- `approve_link_request(p_request_id)` / `reject_link_request(p_request_id)` — рішення ЦІЛІ
  (`target_user_id = auth.uid()`). `approve` викликає `perform_*_link` (тепер від імені
  вимагача, `p_requester_user_id` передається явно, а не через `auth.uid()`) і сповіщає
  вимагача типом `LINK_REQUEST_APPROVED`; `reject` лише позначає рядок — без сповіщення
  вимагачу (щоб не підтверджувати зловмиснику існування акаунта за номером).
- Клієнт викликає це через `repository.linkToRegisteredUser(id)` (email/phone-спроба) і
  `notificationRepository.approveLinkRequest`/`rejectLinkRequest(requestId)` (рішення цілі
  з `NotificationsScreen`, кнопки на рядку типу `LINK_REQUEST`) — усе **online-only RPC, не
  Room**. Оновлені рядки повертаються назад звичайним Realtime-pull, окремо в Room не пишуться.

### Нові колонки
`debtors`/`creditors`: `linked_user_id`, `mirror_creditor_id`/`mirror_debtor_id`.
`debt_transactions`/`creditor_transactions`: `mirror_transaction_id`.
`notifications`: `related_link_request_id` (0013) + типи `LINK_REQUEST`/`LINK_REQUEST_APPROVED`.
Нова таблиця `pending_link_requests` (0013): `kind`, `source_id`, `requester_user_id`,
`target_user_id`, `status` (`pending`/`approved`/`rejected`).

---

## 10. `notifications` — polling, а не Realtime (`NotificationsPoller`)

> Опитує таблицю `notifications` кожні 15 с, поки є активна сесія (Account+Sync — Local-only
> не бере участі в дзеркалюванні боргів взагалі) і показує системне сповіщення
> (`LocalNotifier`) для кожного нового рядка. За зразком `SyncCoordinator.start` —
> `collectLatest` на `AuthRepository.isAuthenticated` сам зупиняє/перезапускає цикл при
> вході/виході. **Навмисно НЕ Realtime — за явним запитом**: клієнт-платформа сама опитує
> раз на 15 с, а не підписується.

- Курсор — `appSettings.lastSeenNotificationAt` (ISO-8601 рядок).
- `poll()` виконує `fetchSince(lastSeen)` і `unreadCount()` **паралельно** (`async`).
- Якщо `notificationsEnabled` вимкнено користувачем — курсор усе одно рухається
  (щоб повторне ввімкнення не показало лавину), лічильник непрочитаних оновлюється незалежно.
- `fetchSince` повертає найновіші спочатку → `fresh.first()` і є новим значенням курсора.
- Запускається на всіх платформах, **включно з Web** (`webApp/.../main.kt`), на відміну від `SyncCoordinator`.

Пов'язаний: `DueReminderCoordinator` — локальні нагадування про наближення `due_date`
(теж `start()` на процес, теж підписка на auth).

---

## 11. Хто що запускає (`start()`)

| Компонент              | Android | Desktop | iOS | Web |
|------------------------|:-------:|:-------:|:---:|:---:|
| `SyncCoordinator`      | ✅ `DebtTrackerApplication` | ✅ `main.kt` | ✅ `iosMain/main.kt` | ❌ (online-only) |
| `NotificationsPoller`  | ✅ | ✅ | ✅ | ✅ |
| `DueReminderCoordinator` | ✅ | ✅ | ✅ | ✅ |

---

## 12. Короткий підсумок дизайн-рішень

1. **Один coroutine-цикл замість WorkManager** — простіше, крос-платформенно, легко замінити.
2. **Push раз на 30 с + миттєвий push через `refreshNow()`** на pull-to-refresh.
3. **Pull — Realtime `selectAsFlow`**, обгорнутий у `resilient {}` через баг supabase-kt.
4. **LWW за `updatedAt`** лише для метаданих боржника/кредитора; транзакції — сліпий upsert по `id`.
5. **`update()`, а не `upsert()`** для існуючих боржників/кредиторів — інакше `ON DELETE CASCADE`
   зносить їхні транзакції.
6. **Soft-delete** скрізь (`isDeleted`), фізичний `DELETE` — лише `deleteAllData()`/`clearLocalCache()`.
7. **Web не має sync-шару взагалі** — прямий Postgrest, `SyncStatusProvider` = заглушка `Synced`.
8. **Дзеркалювання між акаунтами — повністю на сервері** (RPC + тригери), клієнт лише викликає RPC
   і отримує результат звичайним pull.
9. **`notifications` — polling раз на 15 с, свідомо не Realtime.**
10. **Push реагує на реконект мережі** (`pushOnReconnect()`, `connectivity` бібліотека) —
    доповнення до 30-секундного `pushLoop()`, не заміна.
