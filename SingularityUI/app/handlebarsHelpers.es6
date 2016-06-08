let Handlebars;
let moment;
const slice = [].slice;

Handlebars = require('handlebars');

moment = require('moment');

import Utils from './utils';

Handlebars.registerHelper('appRoot', () => config.appRoot);

Handlebars.registerHelper('apiDocs', () => config.apiDocs);

Handlebars.registerHelper('ifEqual', function(v1, v2, options) {
  if (v1 === v2) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('ifNotEqual', function(v1, v2, options) {
  if (v1 !== v2) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('ifLT', function(v1, v2, options) {
  if (v1 < v2) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('ifGT', function(v1, v2, options) {
  if (v1 > v2) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper("ifAll", function() {
  let condition, conditions, j, k, len, options;
  conditions = 2 <= arguments.length ? slice.call(arguments, 0, j = arguments.length - 1) : (j = 0, []), options = arguments[j++];
  for (k = 0, len = conditions.length; k < len; k++) {
    condition = conditions[k];
    if (condition == null) {
      return options.inverse(this);
    }
  }
  return options.fn(this);
});

Handlebars.registerHelper('percentageOf', (v1, v2) => (v1 / v2) * 100);

Handlebars.registerHelper('fixedDecimal', (value, options) => {
  let place;
  if (options.hash.place) {
    place = options.hash.place;
  } else {
    place = 2;
  }
  return +value.toFixed(place);
});

Handlebars.registerHelper('ifTaskInList', function(list, task, options) {
  let j, len, t;
  for (j = 0, len = list.length; j < len; j++) {
    t = list[j];
    if (t.id === task) {
      return options.fn(this);
    }
  }
  return options.inverse(this);
});

Handlebars.registerHelper('ifInSubFilter', function(needle, haystack, options) {
  if (haystack === 'all') {
    return options.fn(this);
  }
  if (haystack.indexOf(needle) !== -1) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('unlessInSubFilter', function(needle, haystack, options) {
  if (haystack === 'all') {
    return options.inverse(this);
  }
  if (haystack.indexOf(needle) === -1) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('withLast', (list, options) => options.fn(_.last(list)));

Handlebars.registerHelper('withFirst', (list, options) => options.fn(list[0]));

Handlebars.registerHelper('timestampFromNow', timestamp => {
  let timeObject;
  if (!timestamp) {
    return '';
  }
  timeObject = moment(timestamp);
  return `${timeObject.fromNow()} (${timeObject.format(window.config.timestampFormat)})`;
});

Handlebars.registerHelper('ifTimestampInPast', function(timestamp, options) {
  let now, timeObject;
  if (!timestamp) {
    return options.inverse(this);
  }
  timeObject = moment(timestamp);
  now = moment();
  if (timeObject.isBefore(now)) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('ifTimestampSecondsInPast', function(timestamp, seconds, options) {
  let past, timeObject;
  if (!timestamp) {
    return options.inverse(this);
  }
  timeObject = moment(timestamp);
  past = moment().subtract(seconds, "seconds");
  if (timeObject.isBefore(past)) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('ifTimestampSecondsInFuture', function(timestamp, seconds, options) {
  let future, timeObject;
  if (!timestamp) {
    return options.inverse(this);
  }
  timeObject = moment(timestamp);
  future = moment().add(seconds, "seconds");
  if (timeObject.isAfter(future)) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('timestampDuration', timestamp => {
  if (!timestamp) {
    return '';
  }
  return moment.duration(timestamp).humanize();
});

Handlebars.registerHelper('timestampFormatted', timestamp => {
  let timeObject;
  if (!timestamp) {
    return '';
  }
  timeObject = moment(timestamp);
  return timeObject.format(window.config.timestampFormat);
});

Handlebars.registerHelper('timestampFormattedWithSeconds', timestamp => {
  let timeObject;
  if (!timestamp) {
    return '';
  }
  timeObject = moment(timestamp);
  return timeObject.format(window.config.timestampWithSecondsFormat);
});

Handlebars.registerHelper('humanizeText', text => Utils.humanizeText(text));

Handlebars.registerHelper('humanizeFileSize', bytes => Utils.humanizeFileSize(bytes));

Handlebars.registerHelper('ifCauseOfFailure', function(task, deploy, options) {
  let thisTaskFailedTheDeploy;
  thisTaskFailedTheDeploy = false;
  deploy.deployResult.deployFailures.map(failure => {
    if (failure.taskId && failure.taskId.id === task.taskId) {
      return thisTaskFailedTheDeploy = true;
    }
  });
  if (thisTaskFailedTheDeploy) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('ifDeployFailureCausedTaskToBeKilled', function(task, options) {
  let deployFailed, taskKilled;
  deployFailed = false;
  taskKilled = false;
  task.taskUpdates.map(update => {
    if (update.statusMessage && update.statusMessage.indexOf('DEPLOY_FAILED' !== -1)) {
      deployFailed = true;
    }
    if (update.taskState === 'TASK_KILLED') {
      return taskKilled = true;
    }
  });
  if (deployFailed && taskKilled) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('causeOfDeployFailure', (task, deploy) => {
  let failureCause;
  failureCause = '';
  deploy.deployResult.deployFailures.map(failure => {
    if (failure.taskId && failure.taskId.id === task.taskId) {
      return failureCause = Handlebars.helpers.humanizeText(failure.reason);
    }
  });
  if (failureCause) {
    return failureCause;
  }
});

Handlebars.registerHelper('usernameFromEmail', email => {
  if (!email) {
    return '';
  }
  return email.split('@')[0];
});

Handlebars.registerHelper('substituteTaskId', (value, taskId) => Utils.substituteTaskId(value, taskId));

Handlebars.registerHelper('filename', value => Utils.fileName(value));

Handlebars.registerHelper('getLabelClass', state => Utils.getLabelClassFromTaskState(state));

Handlebars.registerHelper('trimS3File', (filename, taskId) => {
  let finalRegex;
  if (!config.taskS3LogOmitPrefix) {
    return filename;
  }
  finalRegex = config.taskS3LogOmitPrefix.replace('%taskId', taskId.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&')).replace('%index', '[0-9]+').replace('%s', '[0-9]+');
  return filename.replace(new RegExp(finalRegex), '');
});

Handlebars.registerHelper('isRunningState', function(list, options) {
  switch (_.last(list).taskState) {
    case 'TASK_RUNNING':
      return options.fn(this);
    default:
      return options.inverse(this);
  }
});

Handlebars.registerHelper('isSingularityExecutor', function(value, options) {
  if (value && value.indexOf('singularity-executor' !== -1)) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('lastShellRequestStatus', statuses => {
  if (statuses.length > 0) {
    return statuses[0].updateType;
  }
});

Handlebars.registerHelper('shellRequestOutputFilename', statuses => {
  let j, len, status;
  for (j = 0, len = statuses.length; j < len; j++) {
    status = statuses[j];
    if (status.outputFilename) {
      return status.outputFilename;
    }
  }
});

Handlebars.registerHelper('ifShellRequestHasOutputFilename', function(statuses, options) {
  let j, len, status;
  for (j = 0, len = statuses.length; j < len; j++) {
    status = statuses[j];
    if (status.outputFilename) {
      return options.fn(this);
    }
  }
  return options.inverse(this);
});

Handlebars.registerHelper('reverseEach', function(context, options) {
  let i, ret;
  ret = '';
  if (context && context.length > 0) {
    i = context.length - 1;
    while (i >= 0) {
      ret += options.fn(context[i]);
      i--;
    }
  } else {
    ret = options.inverse(this);
  }
  return ret;
});

Handlebars.registerHelper('ifCurrentState', function(state, updates, options) {
  let lastState;
  lastState = _.last(updates);
  if (lastState.taskState === state) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});
