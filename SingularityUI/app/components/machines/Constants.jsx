import chroma from 'chroma-js';

export const HEALTH_SCALE_MAX = 10000;
export const WHOLE_NUMBER = 0;
export const HUNDREDTHS_PLACE = 2;

export const STAT_NAMES = {
  cpusUsedStat : 'cpusUsed',
  memoryBytesUsedStat : 'memoryBytesUsed',
  numTasksStat : 'numTasks',
  slaveIdStat : 'slaveId',
  timestampStat : 'timestamp',
  diskBytesUsedStat : 'diskBytesUsed'
};

export const SLAVE_HEALTH_MENU_ITEM_ORDER = [
  'host',
  STAT_NAMES.cpusUsedStat,
  STAT_NAMES.memoryBytesUsedStat,
  STAT_NAMES.diskBytesUsedStat,
  STAT_NAMES.numTasksStat,
  STAT_NAMES.timestampStat,
];

export const HEALTH_SCALE = chroma.scale(['3182bd','9ecae1','deebf7','fee0d2','fc9272','de2d26']).colors(HEALTH_SCALE_MAX);
