Operations which are synchronized:
- Mesos scheduler updates (resource offers, task updates, draing pending queue, checking for decomissioned slaves)
- Cleaning requests & cleaning tasks

Operations which are not synchronized:
- Locating tasks directories on slaves
- Creating a new request
- User creates cleanup task for a task
