import React, { PropTypes } from 'react';
import Utils from '../../utils';
import SlaveHealthMenuItems from './SlaveHealthMenuItems';
import { Dropdown } from 'react-bootstrap';
import { SLAVE_STYLES, THRESHOLDS, STAT_NAMES, STAT_STYLES } from './Constants';


const isStatCritical = (slaveInfo, slaveUsage, statName) => {
  switch (statName) {
    case STAT_NAMES.cpusUsedStat:
      return (slaveUsage.cpusUsed / Utils.getMaxAvailableResource(slaveInfo, statName)) > THRESHOLDS.cpusCriticalThreshold;
    case STAT_NAMES.memoryBytesUsedStat:
      return (slaveUsage.memoryBytesUsed / (Utils.getMaxAvailableResource(slaveInfo, statName))) > THRESHOLDS.memoryCriticalThreshold;
    default:
      return false;
  }
};

const isStatWarning = (slaveInfo, slaveUsage, statName) => {
  switch (statName) {
    case STAT_NAMES.cpusUsedStat:
      return (slaveUsage.cpusUsed / Utils.getMaxAvailableResource(slaveInfo, statName)) > THRESHOLDS.cpusWarningThreshold;
    case STAT_NAMES.memoryBytesUsedStat:
      return (slaveUsage.memoryBytesUsed / (Utils.getMaxAvailableResource(slaveInfo, statName))) > THRESHOLDS.memoryWarningThreshold;
    default:
      return false;
  }
};

const getSlaveStyle = (checkedStats) => {
  let style = SLAVE_STYLES.ok;

  if (checkedStats.some((obj) => {return obj.style === STAT_STYLES.critical;})) {
    style = SLAVE_STYLES.critical;
  } else if (checkedStats.some((obj) => {return obj.style === STAT_STYLES.warning;})) {
    style = SLAVE_STYLES.warning;
  }

  return style;
};

const SlaveHealth = ({slaveInfo, slaveUsage}) => {
  const checkStats = (val, stat) => {
    const newStat = {
      name : stat,
      value : (stat === STAT_NAMES.slaveIdStat ? slaveInfo.host : val),
      maybeTotalResource : Utils.isResourceStat(stat) ? Utils.getMaxAvailableResource(slaveInfo, stat) : '',
      style : STAT_STYLES.ok
    };

    if (isStatCritical(slaveInfo, slaveUsage, stat)) {
      newStat.style = STAT_STYLES.critical;
    } else if (isStatWarning(slaveInfo, slaveUsage, stat)) {
      newStat.style = STAT_STYLES.warning;
    }

    return newStat;
  };

  const checkedStats = _.map(slaveUsage, checkStats);

  const slaveStyle = getSlaveStyle(checkedStats);

  return (
    <Dropdown key={slaveUsage.slaveId} id={slaveUsage.slaveId}>
      <Dropdown.Toggle noCaret={true} className={`${slaveStyle} single-slave-btn`} />
      <Dropdown.Menu>
        <SlaveHealthMenuItems stats={checkedStats} />
      </Dropdown.Menu>
    </Dropdown>
  );
};

SlaveHealth.propTypes = {
  slaveUsage : PropTypes.shape({
    slaveId : PropTypes.string.isRequired,
    cpusUsed : PropTypes.number.isRequired,
    memoryBytesUsed : PropTypes.number.isRequired,
    numTasks : PropTypes.number.isRequired,
    timestamp : PropTypes.number.isRequired
  }),
  slaveInfo : PropTypes.shape({
    attributes : PropTypes.object.isRequired,
    resources : PropTypes.object.isRequired
  })
};

export default SlaveHealth;
