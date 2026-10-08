# \# No Snooze Alarm

# 

# \*\*A minimalist alarm app that FORCES you to get up and leaves no room for "just five more minutes."\*\*

# 

# \## Why this app?

# 

# I needed an alarm app that would actually help me GET UP from bed, rather than simply give another button to press while half-asleep.

# 

# At first, I looked for mobile apps with similar ideas, but many of them involved paid subscriptions or had complex user interfaces. All I wanted was a strict alarm app that does its job without making the user figure out \*how\* to use the app first.

# 

# So while building "No Snooze Alarm" I've deliberately ensured it was:

# 

# \- \*\*Simple\*\* — straightforward enough that anyone can understand it.

# \- \*\*Minimal\*\* — no unnecessary features or complicated UI.

# \- \*\*Free\*\* — no subscription or paywall.

# \- \*\*Offline\*\* — no account, backend, or internet connection required.

# \- \*\*Strict\*\* — instead of simply dismissing the alarm, you have to complete challenges to stop it.

# 

# The entire interface was designed with a simple, almost old-fashioned approach, with a black and white interface, and no unnecessary decoration. Claude was used as the coding assistant throughout development.

# 

# \## What does it do?

# 

# Instead of simply dismissing an alarm, the user can choose challenges that they must complete before the alarm will stop.

# 

# Challenges include:

# 

# \- 🧮 Math problems

# \- 🧠 Memory challenges

# \- 🧩 Logic questions

# \- 🚶 Steps / jumps

# \- 📱 Shaking the phone

# \- 📷 Taking a photo in another room

# 

# The user can combine different challenges for each alarm and choose how many you want to complete and how difficult they should be.

# 

# 

# \## How it works

# 

1. # Creating an alarm: the user can choose the time, preferred alarm sound, and background.

# 2\. Choosing wake-up challenges: the user can select the combination of challenges, along with the number of challenges and their difficulty.

# <p align="center">

# &#x20; <img src="docs/screenshots/mission-set.jpg" alt="Setting the mission" width="250">

# </p>



# 3\. Setting optional bedtime reminders: the user can set an ideal bedtime and receive reminders before it to help wind down and get ready for bed.

# 

# <p align="center">

# &#x20; <img src="docs/screenshots/bedtime.jpg" alt="Ideal Bedtime Setting" width="250">

# </p>





# 4\. The alarm locks one hour before it rings — once it's locked, the alarm cannot be edited, deleted, or disabled. This prevents last-minute changes.

# 5\. When the alarm rings, there's only one snooze allowed — the user can snooze the alarm ONCE for 15 minutes. After that, there is no option to snooze, nor  directly dismiss the alarm.

# 

# <p align="center">

# &#x20; <img src="docs/screenshots/alarm.jpg" alt="Alarm ringing screen" width="250">

# </p>





# 7\. The user has to start solving their challenge to silence the alarm. The goal is to get the user physically and mentally engaged rather than allowing the alarm to be dismissed while half-asleep.

# 

# <p align="center">

# &#x20; <img src="docs/screenshots/mission.jpg" alt="Mission solving screen" width="250">

# </p>





# 8\. The challenges could be among: math, memory, logic, physical, or photo challenges. By the time the challenges are completed, the goal is for the user to be awake enough that going straight back to sleep is no longer tempting.

# 9\. Still not wide awake? There's the option to keep going. After completing the initial challenges, the user can choose “I DON'T FEEL WIDE AWAKE YET” to take on additional challenges.

# <p align="center">

# &#x20; <img src="docs/screenshots/notawake1.jpg" alt="Not-awake screen 1" width="250">

# &#x20; <img src="docs/screenshots/notawake2.jpg" alt="Not-awake screen 2" width="250">

# </p>

# 10\. If the user stops interacting with an unfinished challenge for too long, the alarm starts ringing again.

# 

# 

# \## Technologies Used

# 

# \- \*\*Kotlin\*\*

# \- \*\*Jetpack Compose\*\* (UI)

# \- \*\*Android AlarmManager\*\* (Alarm Scheduling)

# \- \*\*Foreground Services\*\* (Reliable alarm ringing in the background)

# \- \*\*Kotlin Serialization\*\* + \*\*Local JSON storage\*\* (Local Data Persistence)

# \- \*\*Android Sensors\*\* (For the steps/jumps and phone shake challenges)

# 

# \## Project Structure

# 

# ```text

# app/

# └── src/main/

# &#x20;   ├── java/com/wake/alarm/

# &#x20;   │   ├── data/

# &#x20;   │   ├── schedule/

# &#x20;   │   ├── ring/

# &#x20;   │   ├── mission/

# &#x20;   │   ├── ui/

# &#x20;   │   └── util/

# &#x20;   │

# &#x20;   ├── assets/

# &#x20;   │   ├── audio/

# &#x20;   │   ├── backgrounds/

# &#x20;   │   └── logic\_questions.json

# &#x20;   │

# &#x20;   └── AndroidManifest.xml

# ```

# 

# \## Privacy

# 

# No Snooze Alarm works entirely on your device.

# 

# \- No account required

# \- No backend

# \- No internet permission

# \- No cloud sync

# \- Alarm data is stored locally

# \- Photos used for the photo challenge are not stored or analysed





# 

# For a deeper explanation of the architecture, alarm scheduling, data storage and Android-specific implementation details, see \[`ARCHITECTURE.md`](ARCHITECTURE.md).

