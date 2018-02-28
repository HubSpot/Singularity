import React, { PropTypes } from 'react';
import StatItem from './StatItem';
import Utils from '../../../utils';
import { STAT_NAMES, SLAVE_HEALTH_MENU_ITEM_ORDER, HUNDREDTHS_PLACE } from '../Constants';

const compareStats = (stat1, stat2) => {
  return SLAVE_HEALTH_MENU_ITEM_ORDER.indexOf(stat1.name) - SLAVE_HEALTH_MENU_ITEM_ORDER.indexOf(stat2.name);
};

const humanizeStatName = (name) => {
  switch (name) {
    case STAT_NAMES.slaveIdStat:
      return 'HOST';
    case STAT_NAMES.cpusUsedStat:
      return 'CPU';
    case STAT_NAMES.memoryBytesUsedStat:
      return 'MEM';
    case STAT_NAMES.diskBytesUsedStat:
      return 'DISK';
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
    case STAT_NAMES.diskBytesUsedStat:
      return `${Utils.humanizeFileSize(value)} / ${Utils.humanizeFileSize(maybeTotalResource)}`;
    case STAT_NAMES.numTasksStat:
      return value.toString();
    default:
      return '';
  }
};

const humanizeStatPct = (name, value, maybeTotalResource) => {
  if (Utils.isResourceStat(name)) {
    return Utils.toDisplayPercentage(value, maybeTotalResource);
  }
  return null;
};

const maybeLink = (name, value) => {
  if (name === STAT_NAMES.slaveIdStat) {
    return {
      href: `tasks/active/all/${value}`,
      title: `All tasks running on host ${value}`
    };
  }

  return null;
};

const SlaveResourceHealthMenuItems = ({stats}) => {
  stats = stats.filter((stat) => _.contains(_.values(STAT_NAMES), stat.name));
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
  stats: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      value: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number,
        PropTypes.object
      ]).isRequired,
      maybeTotalResource: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
      ])
    })
  )
};

export default SlaveResourceHealthMenuItems;
