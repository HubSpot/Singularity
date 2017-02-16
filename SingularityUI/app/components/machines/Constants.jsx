export const HUNDREDTHS_PLACE = 2;

export const SLAVE_STYLES = {
  ok : 'ok-slave',
  warning : 'warning-slave',
  critical : 'critical-slave'
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
  ok : 'ok-stat',
  warning : 'warning-stat',
  critical : 'critical-stat'
};

export const SLAVE_HEALTH_MENU_ITEM_ORDER = [
  'host',
  STAT_NAMES.cpusUsedStat,
  STAT_NAMES.memoryBytesUsedStat,
  STAT_NAMES.numTasksStat,
  STAT_NAMES.timestampStat
];
