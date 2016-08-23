Singularity Log Fetcher
=======================

This log fetcher uses singularity endpoints to find and download log files relevant to a certain, request, task, or deploy. The log fetcher will download (and unzip if needed) all log files for the specified input and search them using grep.

![my log has something to tell you](https://cloud.githubusercontent.com/assets/47152/7101893/b6910826-e03b-11e4-8d25-38ea1b5aa492.gif)

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
|-t , --task-id|Task Id to fetch logs for||
|-r , --request-id|Request Id pattern used to match requests to fetch logs for||
|-T, --task-count|Max number of recent tasks (belonging to a request) to fetch live logs (on machine not s3)|20|
|-d , --deploy-id|Deploy Id to fetch logs for (Must also specify requestId when using this option)|
|-o, --dest|Destination folder for download output|`~/.logfetch_cache`|
|-n --num-parallel-fetches|Max number of log fetches to make at once|10|
|-C, --chunk-size|Chunk size for writing responses to file system|8192|
|-u, --singularity-uri-base|Base url for singularity (e.g. `localhost:8080/singularity/v2/api`)| Must be set!|
|-s , --start|Search for logs no older than this, can be an integer number of days or date in format “%Y-%m-%d %H:%M:%S” or “%Y-%m-%d”, leaving off h-m-s will be inclusive for the current day (00:00:00) | 7 days ago|
|-e , --end|Search for logs no newer than this, can be an integer number of days or date in format “%Y-%m-%d %H:%M:%S” or “%Y-%m-%d”, leaving off h-m-s will be inclusive for the current day (23:59:59)| None (now)|
|-z , --local-zone|Specify times for `-s` and `-e` in your local time zone. If this is not set, times are assumed to be in UTC|unset/false|
|-p, --file-pattern|Should match the executor.s3.uploader.pattern setting, determines if we can match on file name for s3 logs|`%requestId/%Y/%m/%taskId_%index-%s-%filename`|
|-N, --no-name-fetch-off|If a logtype matcher is specified, but the s3 log pattern does not include file name, don't download any s3 files| None (fetch all)|
|-D, --download-only|Only download logs in their current state, don't unzip or grep||
|-g, --grep|Grep string for searching log files(Only for `logfetch`, `logsearch`)||
|-l, --logtype|Glob matcher for type of log file to download| None (match all)|
|-S, --skip-s3|Don't search/download s3 logs|false|
|-L, --skip-live|Don't search/download live logs|false|
|-U, --use-cache|Don't redownload live logs, prefer the cached version|false|
|--search|Run logsearch on the cache of local files (no downloading)|false|
|-i, --show-file-info|Show the parsed timestamp and file name before printing log lines, even if not in verbose mode|false|
|-V, --verbose|More verbose output|false|
|--silent|No output except for log content, overrides -V|false|

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

`logfetch -r 'My_Request_Id' -l '*.out'`
`logfetch -r 'My_Request_Id' -l 'access.log'`

#Logtail
You can tail live log files using `logtail`. Just provide the request, task, or request and deploy along with a log file path.

For example, to tail the `service.log` file for all tasks for a request named `MyRequest`, you would use the command:

`logtail -r 'MyRequest' -l 'service.log'`

- The path for the log file is relative to the base path for that task's sandbox. For example, to tail a file in `(sandbox path)/logs/access.log`, the argument to -l would be `logs/access.log`

As of `0.25.0` a grep option is no longer supported in `logtail`. it more efficient/usable, and therefore recommended, to pipe output to grep for this type of functionality.

##Options
|Flags|Description|Default|
|:---:|:---------|:-----:|
|-f , --conf-folder|Folder to look for configuration files|`~/.logfetch`|
|-c , --conf-file|configuration file to use(path relative to conf_folder)|default|
|-t , --task-id|Task Id to fetch logs for||
|-r , --request-id|Request Id pattern used to match requests to fetch logs for||
|-d , --deploy-id|Deploy Id to fetch logs for (Must also specify requestId when using this option)||
|-u, --singularity-uri-base|Base url for singularity (e.g. `localhost:8080/singularity/v2/api`)|Must be set!|
|-l, --logfile|Log file path to tail (ie logs/access.log)|Must be set!|
|-V, --verbose|Extra output about the task id associated with logs in the output|False|
|--silent|No output except for log content, overrides -V|false|

#Logsearch

An offline version of `logfetch` that will aid in searching through your directory of cached files. The syntax is the same as for `logfetch` with a smaller list of options, shown below:

##Options
|Flags|Description|Default|
|:---:|:---------|:-----:|
|-f , --conf-folder|Folder to look for configuration files|`~/.logfetch`|
|-c , --conf-file|configuration file to use(path relative to conf_folder)|default|
|-t , --task-id|Task Id to fetch logs for||
|-r , --request-id|Request Id pattern used to match requests to fetch logs for||
|-d , --deploy-id|Deploy Id to fetch logs for (Must also specify requestId when using this option)|
|-o, --dest|Cache folder to search|`~/.logfetch_cache`|
|-s , --start|Search for logs no older than this, can be an integer number of days or date in format “%Y-%m-%d %H:%M:%S” or “%Y-%m-%d”, leaving off h-m-s will be inclusive for the current day (00:00:00) | 7 days ago|
|-e , --end|Search for logs no newer than this, can be an integer number of days or date in format “%Y-%m-%d %H:%M:%S” or “%Y-%m-%d”, leaving off h-m-s will be inclusive for the current day (23:59:59)| None (now)|
|-z , --local-zone|Specify times for `-s` and `-e` in your local time zone. If this is not set, times are assumed to be in UTC|unset/false|
|-p, --file-pattern|Should match the executor.s3.uploader.pattern setting, determines if we can match on file name for s3 logs|`%requestId/%Y/%m/%taskId_%index-%s-%filename`|
|-g, --grep|Grep string for searching log files||
|-l, --logtype|Glob matcher for type of log file to download| None (match all)|
|-V, --verbose|More verbose output|false|
|--silent|No output except for log content, overrides -V|false|

example:

- grep in logs matching `*.out` logs from request `My_Request_Id`

`logfetch -r 'My_Request_Id' -l '*.out' -g 'Regex_here'`

