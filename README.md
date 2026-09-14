# Zeb Pharmacy — Android v2.0

**Built by Arsalan**

A local-first English/Urdu pharmacy management app for Android.

## v2 features

- English / Urdu switch inside the app.
- `Zeb Pharmacy` branding and `Built by Arsalan` on every standard screen.
- Dashboard with today's net sales, net profit/loss, cash movement, outstanding Qarz, supplier payable, low stock, expiry alerts and stock value.
- Medicine inventory with medicine/generic/manufacturer, batch, expiry, purchase cost, sale price, quantity, low-stock threshold, unit and barcode.
- Inventory search by medicine/generic/manufacturer/barcode and stock adjustment history.
- POS sales with cart, quantity, discount, cash or Qarz payment, partial payment and automatic stock deduction.
- Camera barcode capture in POS using on-device ML Kit Barcode Scanning.
- Qarzadar customer records, current balance, complete ledger and repayments.
- Supplier balances/payables and post-purchase supplier payment recording. Payments are allocated against the oldest outstanding supplier invoices and affect cash, not profit.
- Sale returns/refunds with stock restoration, profit/COGS reversal, and correct cash-vs-Qarz handling.
- Expense/cashflow records.
- Reports for Today, This Month, This Year or any custom `YYYY-MM-DD` date range.
- Report metrics include net sales after returns, sale returns, purchases, COGS, gross profit, expenses, net profit/loss, Qarz given/recovered/outstanding, supplier payable, cash movement and stock value.
- Drill-down sales, returns, purchase and expense history.
- PDF and CSV report export through Android's document picker (Google Drive, OneDrive, Dropbox/device storage when installed and exposed by Android).
- Full portable database backup to Android document providers and backup sharing through Gmail/Drive/other installed apps.
- Backup validation before restore.
- v1 → v2 SQLite migration included.

## Editable bill scanning — safety-first workflow

The purchase bill flow is deliberately:

`Scan / choose bill → OCR suggestions → edit every field → validate → final confirmation → Save Purchase`

OCR **never writes directly to inventory**.

Before saving, the operator can edit:

- Purchase date
- Supplier
- Invoice number
- Bill discount
- Tax / other charges
- Amount paid
- Purchase note
- Medicine name
- Generic/composition
- Manufacturer
- Quantity
- Purchase price
- Sale price
- Batch
- Expiry
- Unit
- Barcode

Rows can be removed or added manually. The app shows subtotal and final bill total live. Pressing **Save Purchase** first opens a final confirmation showing item count, subtotal, discount, charges, total, paid and remaining payable. Stock changes only after that confirmation.

The OCR implementation uses bundled on-device ML Kit Text Recognition. Invoice layouts vary, so OCR is treated only as a data-entry assistant, not an accounting authority.

## Accounting behavior

Net profit is calculated from net sales after returns, actual stored cost of units sold, returned cost reversals and recorded expenses. Qarz is deliberately kept separate from cash.

For purchase invoices, v2 stores the invoice purchase price and also calculates an effective/landed unit cost by proportionally allocating bill-level discounts and extra charges across the purchased items. That cost is used for weighted-average inventory costing and later profit calculations.

Supplier payments reduce cash and outstanding payable, but do not reduce profit a second time. Qarz recovery increases cash without being counted as new revenue. Sale returns reverse the appropriate revenue/cost and either reduce customer Qarz or create a cash refund as applicable.

## Build an APK

Requirements:

- Android Studio with Android SDK 35, or a CI runner with Android SDK support
- JDK 17
- Gradle 8.9 / Android Gradle Plugin 8.7.3

### Android Studio

1. Open this project folder.
2. Allow Gradle sync to finish.
3. Choose **Build → Build APK(s)**.
4. The debug APK is produced under `app/build/outputs/apk/debug/app-debug.apk`.

### GitHub Actions

The project includes `.github/workflows/android-build.yml`.

Push the project to a GitHub repository and run **Build Zeb Pharmacy APK** from Actions (or push to `main`). The workflow uploads an artifact named **Zeb-Pharmacy-APK** containing `app-debug.apk`.

See `APK-BUILD.md` for the short install flow.

## Verification performed for this v2 source

- Android XML resource parsing checked.
- English/Urdu resource key parity checked.
- Manifest activity/source coverage checked.
- Java source syntax-level parsing checks performed (full Android compilation still requires the Android SDK).
- Fresh v2 SQLite schema executed successfully in SQLite.
- v1 schema → v2 migration executed against a seeded database; existing purchase data remained intact and the new cost field was correctly backfilled.

## Current boundaries

- Backup is save/share/restore rather than continuous real-time multi-phone synchronization.
- Barcode scanning is camera capture/read, not a continuous live scanner preview.
- OCR is optimized for Latin-script medicine/invoice text and must always be reviewed before save.
- Sale returns are included; purchase-return-to-supplier workflow is not yet included.
- Staff PIN/role permissions are not yet included.

For a production pharmacy, test with representative real bills, printers/workflows and a copy of real inventory data before making it the sole operational record.
