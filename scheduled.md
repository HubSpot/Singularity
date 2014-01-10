## Future of Scheduled Request Execution

We have discussed alternatives for how scheduled tasks may work in a similar system, or in the future in Singularity. The current system will run overdue tasks (because of the previous task overrunning the next scheduled time) immediately. Most of these are not yet implemented due to complexity concerns both from a design and usage perspective. Most of these potential changes address this key feature:

### Epsilon
An epsilon value would instruct Singularity not to run the task again until the next scheduled time if more than the epsilon value had elapsed. For example, if a task is supposed to run at 1PM every day, and the previous task runs until 2PM, Singularity would immediately run the next task at 2PM. If there was an epsilon value for that Request, and that epsilon was less than one hour, then Singularity would wait until the following day at 1PM, whereas if the epsilon value was larger than the overage (say, 2 hours) than Singularity would run the task immediately.

### Delay vs Period
Scheduled tasks generally fall into one of two categories: tasks that are repeated every X time units or tasks which should run at a given time on certain days. Tasks which are repeated may want to be executed on a particular delay, regardless of the execution time of the task. For example, a task which takes between 3-6 minutes but is running on 5 minute intervals may wish to introduce a 5 minute delay between executions. Currently, that task would experience a delay of between 0-2 minutes between executions, depending on its execution time. We could introduce a delay parameter which could enforce a minimum time between task executions.
 
### Simplifying Repeated Tasks
Building on the delay vs period mentioned above, we could introduce the concept of a simpler schedule expression for the express purpose of jobs which run on repeated intervals. For example, instead of specifying a cron expression one could simply specify "5M" for a schedule, implying this task should be executed every 5 minutes. This could be used in conjunction with the delay parameter and may make it easier to reason about how cron schedules are executed.

### Concurrent scheduled tasks
Request could define a limit as to the number of concurrent scheduled tasks to run at once, similar to instances. This would mean that Singularity could allow more than one instance of a scheduled task to run at once. This is similar to the default way that a cron job works. However, this is probably not desirable as most cron jobs in practice employ some sort of locking to prevent this or rarely if ever run over their time bounds. This would also require additional complexity in the Singularity scheduling system.
