export const SLAVE_TYPES = {
  critical : {
    icon : 'glyphicon glyphicon-remove-sign',
    bsStyle : 'danger'
  },
  warning : {
    icon : 'glyphicon glyphicon-minus-sign',
    bsStyle : 'warning'
  },
  ok : {
    icon : 'glyphicon glyphicon-ok-sign',
    bsStyle : 'success'
  }
};

export const THRESHOLDS = {
  cpusWarningThreshold : 0.80,
  cpusCriticalThreshold : 0.90,
  memoryWarningThreshold : 0.80,
  memoryCriticalThreshold : 0.90,
  numTasksWarning : 100,
  numTasksCritical : 120
};

export const STAT_NAMES = {
  cpusUsedStat : 'cpusUsed',
  memoryBytesUsedStat : 'memoryBytesUsed',
  numTasksStat : 'numTasks',
  slaveIdStat : 'slaveId',
  timestampStat : 'timestamp'
};

export const STAT_STYLES = {
  ok : '',
  warning : 'color-warning',
  critical : 'color-error'
};
