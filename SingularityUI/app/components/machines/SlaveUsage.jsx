import React, { PropTypes } from 'react';
import { Dropdown } from 'react-bootstrap';
import { Glyphicon } from 'react-bootstrap';
import Utils from '../../utils';
import SlaveStat from './SlaveStat';
import { SLAVE_TYPES, THRESHOLDS, STAT_NAMES} from './Constants';

// TODO:
// move threshold props into constants file
// stats all calculated at once into arrayOf(Objects)
  // const stats = [
  //   {
  //     name: 'cpuUsage',
  //     value: 500,
  //     state: 'warning'
  //   }
  // ];
// move helper functions out of component
// improve method naming
  // if it renders, call it render
  // if it has a list, call it somethingList
  // etc.
// better type checking
 // SlaveState.propTypes = {
 //    state: PropTypes.oneOf(Object.keys(STATE_STATES))
 //  };

const SlaveUsage = (props) => {
  const slaveWithStats = (bsStyle, glyphicon) => (
    <Dropdown key={props.slaveInfo.slaveId} id={props.index.toString()}>
      <Dropdown.Toggle bsSize="large" bsStyle={bsStyle} noCaret={true} className="single-slave-btn">
        <Glyphicon glyph={glyphicon} />
      </Dropdown.Toggle>
      <Dropdown.Menu>
        {renderSlaveStats}
      </Dropdown.Menu>
    </Dropdown>
  );

  const isSlaveCritical = () => {
    return isStatCritical(STAT_NAMES.cpusUsedStat) ||
           isStatCritical(STAT_NAMES.memoryBytesUsedStat) ||
           isStatCritical(STAT_NAMES.numTasksStat);
  };

  const isSlaveWarning = () => {
    return isStatWarning(STAT_NAMES.cpusUsedStat) ||
           isStatWarning(STAT_NAMES.memoryBytesUsedStat) ||
           isStatWarning(STAT_NAMES.numTasksStat);
  };

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
    }
  };

  const humanizeStat = (statName) => {
    switch (statName) {
      case STAT_NAMES.memoryBytesUsedStat:
        return Utils.formatUnicorn('Memory used : {0}', Utils.humanizeFileSize(props.slaveUsage[statName]));
      case props.timestampStat:
        return Utils.formatUnicorn('{0} : {1}', Utils.humanizeCamelcase(statName), Utils.absoluteTimestampWithSeconds(props.slaveUsage[statName]));
      case props.slaveIdStat:
        return Utils.formatUnicorn('Host : {host}', props.slaveInfo);
      default:
        return Utils.formatUnicorn('{0} : {1}', Utils.humanizeCamelcase(statName), props.slaveUsage[statName]);
    }
  };

  const checkSlaveStat = (statValue, statName) => {
    let statStyle = null;

    if (isStatCritical(statName)) {
      statStyle = 'color-error';
    } else if (isStatWarning(statName)) {
      statStyle = 'color-warning';
    }

    return <SlaveStat key={statName} name={humanizeStat(statName)} value={statValue.toString()} status={statStyle}/>;
  };

  // TODO
  // const renderSlaveStats = Object.keys(props.slaveUsage).map((data) => <SlaveStat key={statName} name={} value={} status={} />);
  const renderSlaveStats = _.map(props.slaveUsage, checkSlaveStat);

  const drawSlave = () => {
    if (isSlaveCritical()) {
      return slaveWithStats(SLAVE_TYPES.critical.bsStyle, SLAVE_TYPES.critical.icon);
    } else if (isSlaveWarning()) {
      return slaveWithStats(SLAVE_TYPES.warning.bsStyle, SLAVE_TYPES.warning.icon);
    } else {
      return slaveWithStats(SLAVE_TYPES.ok.bsStyle, SLAVE_TYPES.ok.icon);
    }
  };

  return drawSlave();
};


SlaveUsage.propTypes = {
  slaveUsage : PropTypes.shape({
    slaveId  : PropTypes.string.isRequired,
    cpusUsed : PropTypes.number.isRequired,
    memoryBytesUsed : PropTypes.number.isRequired,
    numTasks : PropTypes.number.isRequired,
    timestamp : PropTypes.number.isRequired
  }),
  slaveInfo : PropTypes.shape({
    attributes : PropTypes.object.isRequired,
    resources : PropTypes.object.isRequired
  }),
  index : PropTypes.number.isRequired
};

export default SlaveUsage;
