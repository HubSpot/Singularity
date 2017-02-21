import React, { PropTypes } from 'react';
import StatItem from './StatItem';
import Utils from '../../utils';
import { STAT_NAMES, SLAVE_HEALTH_MENU_ITEM_ORDER, HUNDREDTHS_PLACE } from './Constants';

const compareStats = (a, b) => {
  return SLAVE_HEALTH_MENU_ITEM_ORDER.indexOf(a.name) - SLAVE_HEALTH_MENU_ITEM_ORDER.indexOf(b.name);
};

const humanizeStatName = (name) => {
  switch (name) {
    case STAT_NAMES.slaveIdStat:
      return 'HOST';
    case STAT_NAMES.cpusUsedStat:
      return 'CPU';
    case STAT_NAMES.memoryBytesUsedStat:
      return 'MEM';
    case STAT_NAMES.numTasksStat:
      return 'TASKS';
    case STAT_NAMES.timestampStat:
      return '';
    default:
      throw new Error(`${name} is an unsupported statistic`);
  }
};

const humanizeStatValue = (name, value, maybeTotalResource) => {
  switch (name) {
    case STAT_NAMES.slaveIdStat:
      return Utils.humanizeSlaveHostName(value);
    case STAT_NAMES.cpusUsedStat:
      return `${Utils.roundTo(value, HUNDREDTHS_PLACE)} / ${maybeTotalResource}`;
    case STAT_NAMES.memoryBytesUsedStat:
      return `${Utils.humanizeFileSize(value)} / ${Utils.humanizeFileSize(maybeTotalResource)}`;
    case STAT_NAMES.numTasksStat:
      return value;
    case STAT_NAMES.timestampStat:
      return '';
    default:
      throw new Error(`${name} is an unsupported statistic`);
  }
};

const humanizeStatPct = (name, value, maybeTotalResource) => {
  if (Utils.isResourceStat(name)) {
    return Utils.roundTo((value / maybeTotalResource) * 100, HUNDREDTHS_PLACE);
  }

  return null;
};


const SlaveHealthMenuItems = ({stats}) => {
  const renderSlaveStats = _.map(stats.sort(compareStats), ({name, value, maybeTotalResource, style}) => {
    return <StatItem key={name} name={humanizeStatName(name)} value={humanizeStatValue(name, value, maybeTotalResource)} className={style} percentage={humanizeStatPct(name, value, maybeTotalResource)} />;
  });

  return (
    <div id="slave-stats">
      {renderSlaveStats}
    </div>
  );
};

SlaveHealthMenuItems.propTypes = {
  stats : PropTypes.arrayOf(
    PropTypes.shape({
      name : PropTypes.string.isRequired,
      value : PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
      ]).isRequired,
      maybeTotalResource : PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
      ]).isRequired,
      style : PropTypes.string.isRequired
    })
  )
};

export default SlaveHealthMenuItems;
