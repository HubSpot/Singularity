import React, { PropTypes } from 'react';
import { Dropdown } from 'react-bootstrap';
import { Glyphicon } from 'react-bootstrap';
import Utils from '../../utils';
import SlaveStat from './SlaveStat';
import { SLAVE_TYPES, THRESHOLDS, STAT_NAMES} from './Constants';

// TODO:
// move helper functions out of component

const SlaveUsage = (props) => {
  const getMaxAvailableResource = (statName) => {
    switch (statName) {
      case STAT_NAMES.cpusUsedStat:
        try {
          return parseFloat(props.slaveInfo.attributes.real_cpus) || props.slaveInfo.resources.cpus;
        } catch (e) {
          throw Utils.formatUnicorn('Could not find resource (cpus) for slave {host} ({id})', props.slaveInfo);
        }
      case STAT_NAMES.memoryBytesUsedStat:
        try {
          return parseFloat(props.slaveInfo.attributes.real_memory_mb) || props.slaveInfo.resources.mem;
        } catch (e) {
          throw Utils.formatUnicorn('Could not find resources (memory) for slave {host} ({id})', props.slaveInfo);
        }
      default:
        throw Utils.formatUnicorn('{0} is an unsupported statistic', statName);
    }
  };

  const isStatCritical = (statName) => {
    switch (statName) {
      case STAT_NAMES.cpusUsedStat:
        return (props.slaveUsage.cpusUsed / getMaxAvailableResource(statName)) > THRESHOLDS.cpusCriticalThreshold;
      case STAT_NAMES.memoryBytesUsedStat:
        // todo: create util method to convert from mb to bytes
        return (props.slaveUsage.memoryBytesUsed / (getMaxAvailableResource(statName) * Math.pow(1024, 2))) > THRESHOLDS.memoryCriticalThreshold;
      case STAT_NAMES.numTasksStat:
        return props.slaveUsage.numTasks > THRESHOLDS.numTasksCritical;
      default:
        return false;
    }
  };

  const isStatWarning = (statName) => {
    switch (statName) {
      case STAT_NAMES.cpusUsedStat:
        return (props.slaveUsage.cpusUsed / getMaxAvailableResource(statName)) > THRESHOLDS.cpusWarningThreshold;
      case STAT_NAMES.memoryBytesUsedStat:
        // todo: create util method to convert from mb to bytes
        return (props.slaveUsage.memoryBytesUsed / (getMaxAvailableResource(statName) * Math.pow(1024, 2))) > THRESHOLDS.memoryWarningThreshold;
      case STAT_NAMES.numTasksStat:
        return props.slaveUsage.numTasks > THRESHOLDS.numTasksWarning;
      default:
        return false;
    }
  };

  const humanizeStat = (statName) => {
    switch (statName) {
      case STAT_NAMES.memoryBytesUsedStat:
        return Utils.formatUnicorn('Memory used : {0}', Utils.humanizeFileSize(props.slaveUsage[statName]));
      case STAT_NAMES.timestampStat:
        return Utils.formatUnicorn('{0} : {1}', Utils.humanizeCamelcase(statName), Utils.absoluteTimestampWithSeconds(props.slaveUsage[statName]));
      case STAT_NAMES.slaveIdStat:
        return Utils.formatUnicorn('Host : {host}', props.slaveInfo);
      default:
        return Utils.formatUnicorn('{0} : {1}', Utils.humanizeCamelcase(statName), props.slaveUsage[statName]);
    }
  };

  const checkStats = (val, stat) => {
    const newStat = {
      name : stat,
      value : val,
      status : 'ok'
    };

    if (isStatCritical(stat)) {
      newStat.status = 'critical';
    } else if (isStatWarning(stat)) {
      newStat.status = 'warning';
    }

    return newStat;
  };

  const checkedStats = _.map(props.slaveUsage, checkStats);

  const renderSlaveStats = _.map(checkedStats, ({name, value, status}) => {
    return <SlaveStat key={name} name={humanizeStat(name)} value={value.toString()} status={status} />;
  });

  const getSlaveStyle = () => {
    let style = SLAVE_TYPES.ok;

    if (checkedStats.some((obj) => {
      return obj.status === 'critical';
    })) {
      style = SLAVE_TYPES.critical;
    } else if (checkedStats.some((obj) => {
      return obj.status === 'warning';
    })) {
      style = SLAVE_TYPES.warning;
    }

    return style;
  };

  const slaveStyle = getSlaveStyle();

  return (
    <Dropdown key={props.slaveUsage.slaveId} id={props.slaveUsage.slaveId}>
      <Dropdown.Toggle bsSize="large" bsStyle={slaveStyle.bsStyle} noCaret={true} className="single-slave-btn">
        <Glyphicon glyph={slaveStyle.icon} />
      </Dropdown.Toggle>
      <Dropdown.Menu>
        {renderSlaveStats}
      </Dropdown.Menu>
    </Dropdown>
  );
};


SlaveUsage.propTypes = {
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

export default SlaveUsage;
