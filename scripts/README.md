Singularity Log Fetcher
=======================

This log fetcher uses singularity endpoints to find and download log files relevant to a certain, request, task, or deploy. The log fetcher will download (and unzip if needed) all log files for the specified input and search them using grep. 

##Installation
```
./install.sh
```

##Configuration
- Configuration can either be read from a file, or from command line arguments.
- An example configuration files lives in this directory at .logfetch.example
- Any arguments specified in the log file can be overriden on the command line
- The default conf file localtion is `~/.logfetch` but can be changed via the -c command line option

##Options
|Flags|Descrption|Default|
|:---:|:---------|:-----:|
|-c , --conf_file|Path to configuration file to use|~/.logfetch|
|-t , --taskId|TaskId to fetch logs for|
|-r , --requestId|REquestId to fetch logs for|
|--task-count|Number of recent tasks (belonging to a request) to fetch live logs (on machine not s3)|1|
|-d , --deployId|DeployId to fetch logs for (Must also specify requestId when using this option)|
|--dest|Destination folder for downloaded log files, default is current working directory|~/.logfetch_cache|
|-n --num-parallel-fetches|Max number of log fetches to make at once|5
|-cs, --chunk_size|Chunk size for writing responses to file system|8192
|-s, --singularity-uri-base|Base url for singularity (ie localhost:8080/singularity/v2/api), This MUST be set|
|-g, --grep|Grep string for searching log files|

##Grep and Log Files
When the -g option is set, the log fetcher will grep the downloaded files for the provided regex.
- Syntax is the same as regular grep
- The command being executed is `grep -r (your input regex) (your destination directory)`

##Example Usage
- Search logs for a request
  - `logfetch -r 'My_Jobs_Id' -g 'Regex_here'`
- Search logs for a specific deploy
  - `logfetch -r 'My_Jobs_Id' -d '1_2_3' -g 'Regex_here'`
- Search logs for a specific task
  - `logfetch -t 'My_Task_id' -g 'Regex_here'`
- Specify your own configuration file
  - `logfetch -c /etc/my_conf_file -t 'My_Task_id' -g 'Regex_here'`
- Don't search, just download logs
  - `logfetch -r 'My_Jobs_Id'`
