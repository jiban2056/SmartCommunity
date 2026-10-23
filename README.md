# Smart Community Services Platform

## ICT602 Assessment 3 Part A

A Java command-line prototype for Greenfield City Council’s
Service Request Management module.

Group members:
- Jiban Kumal
- Varun
- Amrit

## 1. Requirements

- JDK 21 installed.
- Command Prompt on Windows.
- VS Code is optional.
- No external Java libraries or database installation are required.

Check Java installation:

```bat
java -version
javac -version
```

Both commands should identify Java 21.

## 2. Project Structure

- src/Main.java — application entry point and dependency setup.
- src/presentation/ — command-line interface.
- src/application/ — services, session management and interfaces.
- src/domain/ — domain classes and business rules.
- src/infrastructure/ — demo accounts, file storage and notifications.
- checks/ — standalone verification programs.
- data/requests.bin — saved request records.
- data/evidence/ — copied evidence files.
- demo-completion.txt — sample completion evidence.
- out/ — compiled classes generated during compilation.

LocalEvidenceStorage.java belongs in src/infrastructure/.

## 3. Extract and Open the Project

1. Extract SmartCommunity.zip into a writable folder.
2. Open the extracted SmartCommunity folder in VS Code.
3. Open Terminal > New Terminal and select Command Prompt.
4. Ensure the terminal is in the folder containing src and checks.

Alternatively, open Command Prompt directly in that folder.

Do not run the project from inside the ZIP archive.

## 4. Compile

Run this command from the project root:

```bat
javac -d out src\domain\*.java src\application\*.java src\infrastructure\*.java src\presentation\*.java src\Main.java
```

Successful compilation normally produces no message.

Compile again after changing source code.

## 5. Start the Application

```bat
java -cp out Main
```

At the opening menu:
- Enter 1 to log in.
- Enter 0 to exit.

Enter the email only after the Email prompt appears.

## 6. Demo Accounts

These are public demonstration credentials.
Do not use them for real accounts or real personal information.

| Account | Email | Password |
|---|---|---|
| Resident | resident@example.com | Resident123! |
| Other resident | other@example.com | Other123! |
| Council employee | council@example.com | Council123! |
| Maintenance staff, T01 | worker@example.com | Worker123! |
| Other maintenance staff, T02 | outsider@example.com | OtherWorker123! |

Passwords are case-sensitive.

All supplied demo accounts are verified.
Account registration and production identity verification are outside
this prototype's scope.

## 7. Menu Options

The displayed menu depends on the logged-in user's role.

| Option | Action | Authorised user |
|---|---|---|
| 1 | List/filter visible requests | Logged-in users, within access permissions |
| 2 | View request details | Request owner, council employee or latest assigned team |
| 3 | Submit request | Resident |
| 4 | Change priority | Council employee |
| 5 | Assign/reassign team | Council employee |
| 6 | Start work | Assigned maintenance staff |
| 7 | Complete work with evidence | Assigned maintenance staff |
| 8 | Submit feedback | Submitting resident after completion |
| 9 | Escalate immediate safety risk | Council employee |
| L | Log out | Logged-in user |
| 0 | Exit | Any user |

Use uppercase L to log out.

For optional list filters, press Enter to leave them blank.

## 8. Demonstration Workflow

### Step 1 — Submit a request

Log in as resident@example.com and choose option 3.

Example inputs:
- Category: ROAD
- Description: Pothole near the community centre entrance.
- Location: 10 Example Street, Greenfield
- Photo path: press Enter to skip, or provide an existing image path.
- Preferred channel: EMAIL or SMS

Available category IDs:
- ROAD
- WASTE
- GRAFFITI
- LIGHT

Copy the generated REQ- reference exactly.
Use this reference throughout the demonstration.

### Step 2 — Prioritise and assign

Log out using L.
Select 1 and log in as council@example.com.

- Use option 4 to set the request priority.
- Use option 5 to assign a team.

Teams:
- T01: Road Maintenance — worker@example.com
- T02: General Maintenance — outsider@example.com

Provide work instructions and a future due date/time.

Required format:
yyyy-MM-ddTHH:mm

Choose a date later than the current time.
Reassignment is permitted before work starts.

### Step 3 — Start and complete work

For a T01 assignment, log in as worker@example.com.

- Use option 6 to start work.
- Use option 7 to complete it.
- Enter a completion note.
- Supply the path to an existing evidence file.

For the supplied sample file, enter:

demo-completion.txt

This relative path works when the application is launched from the
project root and the sample file is present.

You may also enter an absolute file path from your own computer.
Do not reuse another member's Windows user-folder path.

Completion evidence supports JPG, JPEG, PNG, PDF and TXT files.
Files must be non-empty and no larger than 5 MiB.
Submission photos support JPG, JPEG and PNG only.

### Step 4 — Provide feedback

Log in again as the submitting resident.

- Use option 8.
- Enter the same request reference.
- Submit feedback.

Feedback is allowed only after completion and only once per request.

### Step 5 — Inspect the result

Use option 2 to inspect:
- Current status and priority.
- Assignment history.
- Actor identities and timestamps.
- Evidence file references.
- Simulated notification records.
- Resident feedback.

Email and SMS delivery are simulated.
No real email or text message is sent.

## 9. Check Access Restrictions

Log in as other@example.com.

Attempt to view a request submitted by resident@example.com
using option 2.

Expected outcome:
Access denied for this request.

Option 1 should show only requests visible to the current account.

Maintenance staff outside the assigned team cannot start or
complete that team's work.

## 10. Run the Included Checks

Exit the application first.

Compile the application using Section 4, then compile the checks:

```bat
javac -cp out -d out checks\*.java
```

Run each check separately:

```bat
java -cp out WorkflowCheck
java -cp out AuthenticationCheck
java -cp out PersistenceCheck
```

Review the PASS messages and final summary.
An AssertionError indicates a failed check that needs investigation.

The persistence check uses a temporary directory.
It does not use the demonstration data/requests.bin file.

These checks cover selected scenarios, not every possible defect.

## 11. Saved Data

Request records are stored in:

data/requests.bin

Copied evidence files are stored in:

data/evidence/

Run the application from the same project root each time so that
it uses the same data folder.

To check persistence:
1. Save a request or feedback.
2. Exit using 0.
3. Start the application again.
4. Log in and view the same request reference.

Do not edit requests.bin manually.

Use only one running application instance per project data folder.

## 12. Share with Group Members

1. Save all source files and exit the application.
2. Include src, checks, README.md and demo-completion.txt in the ZIP.
3. Include data if sharing the existing demonstration records.
4. Share the ZIP with the other members.
5. Each member extracts it and follows Sections 1–5.

The out folder is optional because each member should compile
the source on their own computer.

If data is omitted, the application starts with no saved requests.
The required storage folders/files are created when data is saved.

Each extracted copy has independent local data.
There is no shared server, cloud synchronisation or remote login.

## 13. Troubleshooting

### 'javac' is not recognised

Install/configure JDK 21 and ensure its bin folder is on PATH.
Close and reopen the terminal, then check javac -version.

### Could not find or load main class Main

Ensure compilation succeeded and the terminal is in the project root.
Run:

java -cp out Main

### UnsupportedClassVersionError

Check java -version and javac -version.
Use JDK 21 for both compilation and execution, then recompile.

### Choose 1 or 0

Enter 1 first.
Wait for the Email prompt before entering the account email.

### Request not found

Copy the complete generated reference.
Ensure you are running from the correct project folder.

### Access denied

Check the current account, request ownership and assigned team.
Access restrictions are expected behaviour.

### Evidence must be a readable file

Use a file that exists on your own computer.
Check its path, extension and size.

### Invalid due date

Use yyyy-MM-ddTHH:mm and choose a future date/time.

### Could not load requests

Preserve the existing data folder for investigation.
Do not assume the stored records are empty or delete them immediately.

## 14. Prototype Limitations

- Command-line interface.
- Fixed demo accounts.
- Local file persistence for one running instance.
- Simulated email/SMS notifications.
- No shared multi-user server.
- No real GIS, payment or external authentication integrations.
- Booking, event and administrative modules are conceptual scope only.
- Local request and evidence files are not encrypted.
- Evidence extension checks do not validate file content.
- Production security, performance, availability and accessibility
  targets have not been established by this demonstration.