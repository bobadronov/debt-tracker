import SwiftUI
import SharedUI

@main
struct ComposeApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
                // A third-party QR scanner resolving a scanned debttracker://contact link opens
                // the app via this — forwarded into the shared Kotlin code (core/qr/ContactDeepLinks)
                // so DebtTrackerNavGraph can show the same "add as debtor or creditor?" dialog as
                // an in-app camera scan.
                .onOpenURL { url in
                    ContactDeepLinks.shared.onIncomingLink(rawUri: url.absoluteString)
                }
        }
    }
}

struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return MainKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Updates will be handled by Compose
    }
}
