Singularity Log Fetcher
=======================

This log fetcher uses singularity endpoints to find and download log files relevant to a certain, request, task, or deploy. The log fetcher will download (and unzip if needed) all log files for the specified input and search them using grep.

##Installation
```
pip install singularity-logfetch
```

##Configuration
- Configuration can either be read from a file, or from command line arguments.
- An example configuration files lives in this directory at .logfetch.example
- Any arguments specified in the log file can be overriden on the command line
- You can store a number of configuration files for different clusters in the config directory (`~/.logfetch` by default) and choose which config to use with the -c option

#Logfetch and Logcat
Two commands exist for downloading logs. 
- `logfetch` will download and optionally output a grep command for the logs
- `logcat` will download logs and pipe the contents to stdout

##Options
|Flags|Description|Default|
|:---:|:---------|:-----:|
|-f , --conf-folder|Folder to look for configuration files|`~/.logfetch`|
|-c , --conf-file|configuration file to use(path relative to conf_folder)|default|
|-t , --task-id|Task Id to fetch logs for|
|-r , --request-id|Request Id to fetch logs for|
|-tc, --task-count|Number of recent tasks (belonging to a request) to fetch live logs (on machine not s3)|1|
|-d , --deploy-id|Deploy Id to fetch logs for (Must also specify requestId when using this option)|
|-o, --dest|Destination folder for download output|`~/.logfetch_cache`|
|-n --num-parallel-fetches|Max number of log fetches to make at once|5
|-cs, --chunk-size|Chunk size for writing responses to file system|8192
|-u, --singularity-uri-base|Base url for singularity (e.g. `localhost:8080/singularity/v2/api`), This MUST be set|
|-s , --start-days|Search for logs no older than this many days|7
|-e , --end-days|Search for logs no newer than this many days| None (today)
|-g, --grep|Grep string for searching log files(Only for `logfetch`)|
|-l, --logtype|Glob matcher for type of log file to download| None (match all)|

##Grep and Log Files
When the `-g` option is set, the log fetcher will grep the downloaded files for the provided regex.

- you can pass in a full grep command (including options) to run or just the grep regex
- the default command will just be `grep (your regex)` if a full grep command is not supplied

##Example Usage
- Specify a configuration file AND folder to use

`logfetch -r 'My_Jobs_Id' -c somefile -f ~/.somefolder` (uses ~/.somefolder/somefile as config file)

- Specify a configuration file in the default directory

`logfetch -r 'My_Jobs_Id' -c somefile` (uses ~/.logfetch/somefile as config file)

- Search logs for a request

`logfetch -r 'My_Jobs_Id' -g 'Regex_here'`

- Search logs for a specific deploy

`logfetch -r 'My_Jobs_Id' -d '1_2_3' -g 'Regex_here'`

- Search logs for a specific task

`logfetch -t 'My_Task_id' -g 'Regex_here'`

- Specify your own configuration file

`logfetch -c /etc/my_conf_file -t 'My_Task_id' -g 'Regex_here'`

- Don't search, just download logs

`logfetch -r 'My_Request_Id'`

- Only get logs that match a glob or logfile name with the `-l` option

`logfetch -r ‘My_Request_Id’ -l ‘*.out’`
`logfetch -r ‘My_Request_Id’ -l ‘access.log’`

#Logtail
You can tail live log files using `logtail`. Just provide the request, task, or request and deploy along with a log file path.

For example, to tail the `service.log` file for all tasks for a request named `MyRequest`, you would use the command:

`logtail -r 'MyRequest' -l 'service.log'`

- The path for the log file is relative to the base path for that task's sandbox. For example, to tail a file in `(sandbox path)/logs/access.log`, the argument to -l would be `logs/access.log`

You can also provide the `-g` option which will provide the grep string to the singularity API and search the results. You cannot provide a full grep command as in some of the above examples, just a string to match on.

##Options
|Flags|Description|Default|
|:---:|:---------|:-----:|
|-f , --conf-folder|Folder to look for configuration files|`~/.logfetch`|
|-c , --conf-file|configuration file to use(path relative to conf_folder)|default|
|-t , --task-id|Task Id to fetch logs for|
|-r , --request-id|Request Id to fetch logs for|
|-d , --deploy-id|Deploy Id to fetch logs for (Must also specify requestId when using this option)|
|-u, --singularity-uri-base|Base url for singularity (e.g. `localhost:8080/singularity/v2/api`), This MUST be set|
|-g, --grep|Grep string for searching log files|
|-l, --logfile|Log file path to tail (ie logs/access.log)|Must be set!|
|-v, --verbose|Extra output about the task id associated with logs in the output| False|

