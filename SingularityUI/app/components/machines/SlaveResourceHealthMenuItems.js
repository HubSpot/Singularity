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
    default:
      return '';
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
      return value.toString();
    default:
      return '';
  }
};

const humanizeStatPct = (name, value, maybeTotalResource) => {
  if (Utils.isResourceStat(name)) {
    return Utils.roundTo((value / maybeTotalResource) * 100, HUNDREDTHS_PLACE);
  }

  return null;
};

const maybeLink = (name, value) => {
  if (name === STAT_NAMES.slaveIdStat) {
    return { href : `tasks/active/all/${value}`,
             title : `All tasks running on host ${value}`
           };
  }

  return null;
};

const SlaveResourceHealthMenuItems = ({stats}) => {
  const renderSlaveStats = _.map(stats.sort(compareStats), ({name, value, maybeTotalResource}) => {
    return <StatItem key={name} name={humanizeStatName(name)} value={humanizeStatValue(name, value, maybeTotalResource)} maybeLink={maybeLink(name, value)} percentage={humanizeStatPct(name, value, maybeTotalResource)} />;
  });

  return (
    <div id="slave-stats">
      {renderSlaveStats}
      <li className="timestamp-stat">
        <div className="row">
          <div className="col-xs-12">
            Last updated {Utils.timestampFromNow(stats.find((stat) => stat.name === STAT_NAMES.timestampStat).value)}
          </div>
        </div>
      </li>
    </div>
  );
};

SlaveResourceHealthMenuItems.propTypes = {
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
      ])
    })
  )
};

export default SlaveResourceHealthMenuItems;
