import moment from 'moment';
import { STAT_NAMES } from './components/machines/Constants';

const Utils = {
  TERMINAL_TASK_STATES: ['TASK_KILLED', 'TASK_LOST', 'TASK_FAILED', 'TASK_FINISHED', 'TASK_ERROR'],

  DECOMMISION_STATES: ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION'],

  MACHINE_STATES_FOR_REVERT: ['DECOMMISSIONED', 'STARTING_DECOMMISSION', 'ACTIVE', 'FROZEN'],

  GLOB_CHARS: ['*', '!', '?', '[', ']'],

  LONG_RUNNING_IMMEDIATE_CLEANUPS: ['USER_REQUESTED', 'SCALING_DOWN', 'DEPLOY_FAILED', 'NEW_DEPLOY_SUCCEEDED', 'DEPLOY_STEP_FINISHED', 'DEPLOY_CANCELED' , 'TASK_EXCEEDED_TIME_LIMIT', 'UNHEALTHY_NEW_TASK', 'OVERDUE_NEW_TASK', 'USER_REQUESTED_DESTROY', 'PRIORITY_KILL', 'PAUSE'],

  NON_LONG_RUNNING_IMMEDIATE_CLEANUPS: ['USER_REQUESTED', 'DEPLOY_FAILED', 'DEPLOY_CANCELED', 'TASK_EXCEEDED_TIME_LIMIT', 'UNHEALTHY_NEW_TASK', 'OVERDUE_NEW_TASK', 'USER_REQUESTED_DESTROY', 'INCREMENTAL_DEPLOY_FAILED', 'INCREMENTAL_DEPLOY_CANCELLED', 'PRIORITY_KILL', 'PAUSE'],

  DEFAULT_SLAVES_COLUMNS: {'id': true, 'state': true, 'since': true, 'rack': true, 'host': true, 'uptime': true, 'actionUser': true, 'message': true, 'expiring': true},

  isIn(needle, haystack) {
    return !_.isEmpty(haystack) && haystack.indexOf(needle) >= 0;
  },

  humanizeText(text) {
    if (!text) {
      return '';
    }
    text = text.replace(/_/g, ' ');
    text = text.toLowerCase();
    text = text[0].toUpperCase() + text.substr(1);
    return text;
  },

  humanizeFileSize(bytes) {
    const kilo = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    if (bytes === 0) {
      return '0 B';
    }
    const numberOfPowers = Math.min(Math.floor(Math.log(bytes) / Math.log(kilo)), sizes.length - 1);
    return `${+(bytes / Math.pow(kilo, numberOfPowers)).toFixed(2)} ${sizes[numberOfPowers]}`;
  },

  humanizeCamelcase(text) {
    return text.replace(/^[a-z]|[A-Z]/g, (character, key) => (
      key === 0 ? character.toUpperCase() : ` ${character.toLowerCase()}`
    ));
  },

  humanizeSlaveHostName(longHostName, override=false) {
    return (config.shortenSlaveUsageHostname || override ? longHostName.split('.')[0] : longHostName);
  },

  timestampFromNow(millis) {
    const timeObject = moment(millis);
    return `${timeObject.fromNow()} (${timeObject.format(window.config.timestampFormat)})`;
  },

  timestampFromNowTextOnly(millis) {
    const timeObject = moment(millis);
    return `${timeObject.fromNow()}`;
  },

  absoluteTimestamp(millis) {
    return moment(millis).format(window.config.timestampFormat);
  },

  absoluteTimestampWithSeconds(millis) {
    return moment(millis).format(window.config.timestampWithSecondsFormat);
  },

  timestampWithinSeconds(timestamp, seconds) {
    const before = moment().subtract(seconds, 'seconds');
    const after = moment().add(seconds, 'seconds');
    return moment(timestamp).isBetween(before, after);
  },

  duration(millis) {
    return moment.duration(millis).humanize();
  },

  tailerPath(taskId, logpath) {
    return `task/${taskId}/tail/${Utils.substituteTaskId(logpath, taskId)}`;
  },

  substituteTaskId(value, taskId) {
    return value.replace('$TASK_ID', taskId);
  },

  getLabelClassFromTaskState(state) {
    switch (state) {
      case 'TASK_STAGING':
      case 'TASK_LAUNCHED':
      case 'TASK_STARTING':
      case 'TASK_CLEANING':
        return 'info';
      case 'TASK_FINISHED':
      case 'TASK_KILLED':
        return 'primary';
      case 'TASK_RUNNING':
        return 'success';
      case 'TASK_LOST':
      case 'TASK_FAILED':
      case 'TASK_LOST_WHILE_DOWN':
      case 'TASK_ERROR':
        return 'danger';
      case 'TASK_OVERDUE':
        return 'warning';
      case 'TASK_SCHEDULED':
      case 'TASK_PENDING':
        return 'default';
      default:
        return 'danger'; // Unknown state.
    }
  },

  fileName(filePath) {
    return filePath.substring(filePath.lastIndexOf('/') + 1);
  },

  isGlobFilter(filter) {
    for (const char of this.GLOB_CHARS) {
      if (filter.indexOf(char) !== -1) {
        return true;
      }
    }
    return false;
  },

  fuzzyFilter(filter, fuzzyObjects) {
    const maxScore = _.max(fuzzyObjects, (fuzzyObject) => fuzzyObject.score).score;
    _.chain(fuzzyObjects).map((fuzzyObject) => {
        if (fuzzyObject.original.id.toLowerCase().startsWith(filter.toLowerCase())) {
          fuzzyObject.score = fuzzyObject.score * 10;
        } else if (fuzzyObject.original.id.toLowerCase().indexOf(filter.toLowerCase()) > -1) {
          fuzzyObject.score = fuzzyObject.score * 5;
        }
        return fuzzyObject;
    });
    return _.uniq(
      _.pluck(
        _.sortBy(
          _.filter(
            fuzzyObjects,
            (fuzzyObject) => {
              return fuzzyObject.score > (maxScore / 10) && fuzzyObject.score > 20;
            }
          ),
          (fuzzyObject) => {
            return fuzzyObject.score;
          }
        ).reverse(),
        'original'
      )
    );
  },

  convertMapFromObjectToArray(mapAsObj) {
    const mapAsArray = [];
    for (const key of _.keys(mapAsObj)) {
      mapAsArray.push({ key, value: mapAsObj[key] });
    }
    return mapAsArray;
  },

  convertMapFromArrayToObject(mapAsArray) {
    const mapAsObj = {};
    for (const pair of mapAsArray) {
      mapAsObj[pair.key] = pair.value;
    }
    return mapAsObj;
  },

  getTaskDataFromTaskId(taskId) {
    const splits = taskId.split('-');
    return {
      id: taskId,
      rackId: splits[splits.length - 1],
      host: splits[splits.length - 2],
      instanceNo: splits[splits.length - 3],
      startedAt: splits[splits.length - 4],
      deployId: splits[splits.length - 5],
      requestId: splits.slice(0, +(splits.length - 6) + 1 || 9e9).join('-')
    };
  },

  getMaxAvailableResource(slaveInfo, statName) {
    switch (statName) {
      case STAT_NAMES.cpusUsedStat:
        try {
          return parseFloat(slaveInfo.attributes.real_cpus || slaveInfo.resources.cpus);
        } catch (e) {
          throw new Error(`Could not find resource (cpus) for slave ${slaveInfo.host} (${slaveInfo.id})`);
        }
      case STAT_NAMES.memoryBytesUsedStat:
        try {
          return parseFloat(slaveInfo.attributes.real_memory_mb || slaveInfo.resources.mem) * Math.pow(1024, 2);
        } catch (e) {
          throw new Error(`Could not find resource (memory) for slave ${slaveInfo.host} (${slaveInfo.id})`);
        }
      default:
        throw new Error(`${statName} is an unsupported statistic'`);
    }
  },

  isResourceStat(stat) {
    return stat === STAT_NAMES.cpusUsedStat || stat === STAT_NAMES.memoryBytesUsedStat;
  },

  getRequestIdFromTaskId(taskId) {
    const splits = taskId.split('-');
    return splits.slice(0, splits.length - 5).join('-');
  },

  getInstanceNoFromTaskId(taskId) {
    const splits = taskId.split('-')
    return splits[splits.length-3];
  },

  deepClone(objectToClone) {
    return $.extend(true, {}, objectToClone);
  },

  ignore404(response) {
    if (response.status === 404) {
      app.caughtError();
    }
  },

  joinPath(firstPart, secondPart) {
    if (!firstPart.endsWith('/')) firstPart += '/';
    if (secondPart.startsWith('/')) secondPart = secondPart.substring(1);
    return `${firstPart}${secondPart}`;
  },

  range(begin, end, interval = 1) {
    const res = [];
    for (let currentValue = begin; currentValue < end; currentValue += interval) {
      res.push(currentValue);
    }
    return res;
  },

  trimS3File(filename, taskId) {
    if (!config.taskS3LogOmitPrefix) {
      return filename;
    }
    const finalRegex = config.taskS3LogOmitPrefix.replace('%taskId', taskId.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&')).replace('%index', '[0-9]+').replace('%s', '[0-9]+');
    return filename.replace(new RegExp(finalRegex), '');
  },

  roundTo(value, place) {
    return +(Math.round(parseFloat(value) + 'e+' + place) + 'e-' + place);
  },

  toDisplayPercentage(usage, total) {
    return Utils.roundTo((usage / total) * 100, 2);
  },

  millisecondsToSecondsRoundToTenth(millis) {
    return Math.round(millis / 100) / 10;
  },

  isCauseOfFailure(task, deploy) {
    for (const failure of deploy.deployResult.deployFailures) {
      if (failure.taskId && failure.taskId.id === task.task.taskId.id) {
        return true;
      }
    }
    return false;
  },

  causeOfDeployFailure(task, deploy) {
    for (const failure of deploy.deployResult.deployFailures) {
      if (failure.taskId && failure.taskId.id === task.task.taskId.id) {
        return this.humanizeText(failure.reason);
      }
    }
    return '';
  },

  ifDeployFailureCausedTaskToBeKilled(task) {
    let deployFailed = false;
    let taskKilled = false;
    for (const update of task.taskUpdates) {
      if (update.statusMessage && update.statusMessage.indexOf('DEPLOY_FAILED' !== -1)) {
        deployFailed = true;
      }
      if (update.taskState === 'TASK_KILLED') {
        taskKilled = true;
      }
    }
    return deployFailed && taskKilled;
  },

  healthcheckPort(healthcheckOptions, ports) {
    if (healthcheckOptions) {
      if (healthcheckOptions.portNumber) {
        return healthcheckOptions.portNumber;
      } else if (healthcheckOptions.portIndex && ports.length > healthcheckOptions.portIndex) {
        return ports[healthcheckOptions.portIndex];
      } else {
        return _.first(ports);
      }
    } else {
      return _.first(ports);
    }
  },

  healthcheckTimeout(healthcheckOptions) {
    if (healthcheckOptions) {
      let startupTimeout = healthcheckOptions.startupTimeoutSeconds || config.defaultStartupTimeoutSeconds;
      let attempts = (healthcheckOptions.maxRetries || config.defaultHealthcheckMaxRetries) + 1
      return startupTimeout + (attempts * (healthcheckOptions.intervalSeconds || config.defaultHealthcheckIntervalSeconds))
    } else {
      return config.defaultStartupTimeoutSeconds + ((config.defaultHealthcheckMaxRetries + 1) * config.defaultHealthcheckIntervalSeconds);
    }
  },

  maybe(object, path, defaultValue = undefined) {
    if (!path.length) {
      return object;
    }
    if (!object) {
      return defaultValue;
    }
    if (object.hasOwnProperty(path[0])) {
      return Utils.maybe(
        object[path[0]],
        path.slice(1, path.length),
        defaultValue
      );
    }

    return defaultValue;
  },

  api: {
    isFirstLoad: (api) => {
      return !api || (
        api.isFetching &&
        !api.error &&
        !api.receivedAt
      );
    }
  },
  task: {
    instanceBreakdown: (tasks) => {
      const taskStates = {
        TASK_LAUNCHED: 0,
        TASK_STAGING: 0,
        TASK_STARTING: 0,
        TASK_RUNNING: 0,
        TASK_CLEANING: 0,
        TASK_KILLING: 0,
        TASK_FINISHED: 0,
        TASK_FAILED: 0,
        TASK_KILLED: 0,
        TASK_LOST: 0,
        TASK_LOST_WHILE_DOWN: 0,
        TASK_ERROR: 0
      };

      tasks.forEach((task) => {
        taskStates[task.lastTaskState] = (taskStates[task.lastTaskState] || 0) + 1;
      });

      return taskStates;
    }
  },
  request: {
    // all of these expect a RequestParent object
    LONG_RUNNING_TYPES: new Set(['WORKER', 'SERVICE']),
    hasActiveDeploy: (requestParent) => {
      return Utils.maybe(requestParent, ['activeDeploy'], false) || Utils.maybe(requestParent, ['requestDeployState', 'activeDeploy'], false);
    },
    isDeploying: (requestParent) => {
      return Utils.maybe(requestParent, ['pendingDeploy'], false);
    },
    isLongRunning: (requestParent) => {
      return Utils.request.LONG_RUNNING_TYPES.has(requestParent.request.requestType);
    },
    canBeRunNow: (requestParent) => {
      return requestParent.state === 'ACTIVE'
        && new Set(['SCHEDULED', 'ON_DEMAND']).has(requestParent.request.requestType)
        && Utils.request.hasActiveDeploy(requestParent);
    },
    canBeBounced: (requestParent) => {
      return new Set(['ACTIVE', 'SYSTEM_COOLDOWN']).has(requestParent.state)
        && Utils.request.isLongRunning(requestParent);
    },
    canBeScaled: (requestParent) => {
      return new Set(['ACTIVE', 'SYSTEM_COOLDOWN']).has(requestParent.state)
        && Utils.request.hasActiveDeploy(requestParent)
        && Utils.request.isLongRunning(requestParent);
    },
    runningInstanceCount: (activeTasksForRequest) => {
      return activeTasksForRequest.filter(
        (task) => task.lastTaskState === 'TASK_RUNNING'
      ).length;
    },
    deployingInstanceCount: (requestParent, activeTasksForRequest) => {
      if (!requestParent.pendingDeploy) {
        return 0;
      }
      return activeTasksForRequest.filter((task) => (
        task.lastTaskState === 'TASK_RUNNING'
        && task.taskId.deployId === requestParent.pendingDeploy.id
      )).length;
    },
    // other
    canDisableHealthchecks: (requestParent) => {
      return !!requestParent.activeDeploy
        && !!requestParent.activeDeploy.healthcheck
        && !!requestParent.activeDeploy.healthcheck.uri
        && requestParent.state !== 'PAUSED'
        && !requestParent.expiringSkipHealthchecks;
    },
    pauseDisabled: (requestParent) => {
      const expiringPause = Utils.maybe(requestParent, 'expiringPause');
      return expiringPause
        ? (expiringPause.startMillis + expiringPause.expiringAPIRequestObject.durationMillis) > new Date().getTime()
        : false;
    },
    scaleDisabled: (requestParent) => {
      const expiringScale = Utils.maybe(requestParent, 'expiringScale');
      return expiringScale
        ? (expiringScale.startMillis + expiringScale.expiringAPIRequestObject.durationMillis) > new Date().getTime()
        : false;
    },
    bounceDisabled: (requestParent) => {
      const expiringBounce = Utils.maybe(requestParent, 'expiringBounce');
      return expiringBounce
        ? (expiringBounce.startMillis + expiringBounce.expiringAPIRequestObject.durationMillis) > new Date().getTime()
        : false;
    },
  },

  isImmediateCleanup: (cleanupType, longRunning) => {
    if (longRunning) {
      return _.contains(Utils.LONG_RUNNING_IMMEDIATE_CLEANUPS, cleanupType)
    } else {
      return _.contains(Utils.NON_LONG_RUNNING_IMMEDIATE_CLEANUPS, cleanupType)
    }
  },

  isActiveSlave(slaveInfo) {
    return !Utils.isIn(slaveInfo.currentState.state, ['DEAD', 'MISSING_ON_STARTUP']);
  },

  glyphiconForRequestState: (state) => {
    return {
      'DELETING': {'color': 'color-grey', 'icon':'trash'},
      'ACTIVE': {'color': 'color-success', 'icon':'ok'},
      'PAUSED': {'color': 'color-paused', 'icon':'pause'},
      'SYSTEM_COOLDOWN': {'color': 'color-warning', 'icon':'warning-sign'},
      'PENDING': {'color': 'color-info', 'icon':'hourglass'},
      'CLEANING': {'color': 'color-cleaning', 'icon':'erase'},
    }[state] || {'color': 'color-info', 'icon':'question-sign'}
  },

  enums: {
    SingularityRequestTypes: ['SERVICE', 'WORKER', 'SCHEDULED', 'ON_DEMAND', 'RUN_ONCE'],
    SingularityEmailDestination: ['OWNERS', 'ACTION_TAKER', 'ADMINS'],
    SingularityEmailType: [
      'TASK_LOST',
      'TASK_KILLED',
      'TASK_FINISHED_SCHEDULED',
      'TASK_FINISHED_LONG_RUNNING',
      'TASK_FINISHED_ON_DEMAND',
      'TASK_FINISHED_RUN_ONCE',
      'TASK_FAILED',
      'TASK_SCHEDULED_OVERDUE_TO_FINISH',
      'TASK_KILLED_DECOMISSIONED',
      'TASK_KILLED_UNHEALTHY',
      'REQUEST_IN_COOLDOWN',
      'SINGULARITY_ABORTING',
      'REQUEST_REMOVED',
      'REQUEST_PAUSED',
      'REQUEST_UNPAUSED',
      'REQUEST_SCALED',
      'TASK_FAILED_DECOMISSIONED'
    ]
  },

  queryParams(source) {
    const array = [];
    for (const key in source) {
      if (source[key]) {
        array.push(`${encodeURIComponent(key)}=${encodeURIComponent(source[key])}`);
      }
    }
    return array.join('&');
  },

  getAuthTokenHeader() {
    if (!config.authCookieName) {
      return null;
    }
    const encodedKey = encodeURIComponent(config.authCookieName).replace(/[\-\.\+\*]/g, '\\$&');
    const authCookie = decodeURIComponent(document.cookie.replace(new RegExp(`(?:(?:^|.*;)\\s*${encodedKey}\\s*\\=\\s*([^;]*).*$)|^.*$`), '$1')) || null;
    if (!authCookie) {
      return '';
    }
    const authToken = JSON.parse(authCookie)[config.authTokenKey];
    return `Bearer ${ authToken }`;
  },

  template(template, data) {
    const start = "{{";
    const end = "}}";
    const path = "[a-z0-9_$][\\.a-z0-9_]*";
    const pattern = new RegExp(start + "\\s*("+ path +")\\s*" + end, "gi");
    try {
      return template.replace(pattern, (tag, token) => {
        const tokenPath = token.split(".");
        let value = data;
        let i = 0;

        for (; i < tokenPath.length; i++){
          value = value[tokenPath[i]];
          if (value == null){
            throw tokenPath[i] + "' not found in " + tag;
          }
          if (i === tokenPath.length - 1){
            return value;
          }
        }
      });
    } catch (err) {
      return null;
    }
  }

};

export default Utils;
