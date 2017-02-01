import React, { PropTypes } from 'react';
import { Dropdown } from 'react-bootstrap';
import { Glyphicon } from 'react-bootstrap';
import Utils from '../../utils';
import { connect } from 'react-redux';
import { FetchSlaveUsages, FetchSlaves } from '../../actions/api/slaves';
import CopyToClipboard from 'react-copy-to-clipboard';

const SlaveUsage = (props) => {
  const drawSlave = () => {
    var criticalGlyph = 'glyphicon glyphicon-remove-sign';
    var warningGlyph = 'glyphicon glyphicon-minus-sign';
    var okGlyph = 'glyphicon glyphicon-ok-sign';
    var criticalStyle = 'danger';
    var warningStyle = 'warning';
    var okStyle = 'success';

    if (isSlaveCritical()) {
      return slaveWithStats(criticalStyle, criticalGlyph);
    } else if (isSlaveWarning()) {
      return slaveWithStats(warningStyle, warningGlyph);
    } else {
      return slaveWithStats(okStyle, okGlyph);
    }
  };

  const slaveWithStats = (bsStyle, glyphicon) => (
    <Dropdown key={props.slaveInfo.slaveId} id={props.index.toString()}>
      <Dropdown.Toggle bsSize='large' bsStyle={bsStyle} noCaret={true} className='single-slave-btn'>
        <Glyphicon glyph={glyphicon} />
      </Dropdown.Toggle>
      <Dropdown.Menu>
        {checkSlaveStats()}
      </Dropdown.Menu>
    </Dropdown>
  );

  const checkSlaveStats = () => {
    return Object.keys(props.slaveUsage).map((slaveStat) => {
      return checkSlaveStat(slaveStat);
    });
  };

  const checkSlaveStat = (statName) => {
    var critical = 'color-error';
    var warning = 'color-warning';

    if (isStatCritical(statName)) {
      return slaveStat(statName, critical);
    } else if (isStatWarning(statName, props)) {
      return slaveStat(statName, warning);
    }

    return slaveStat(statName, null);
  };

  const slaveStat = (statName, className) => (
    <CopyToClipboard key={statName + props.index} text={props.slaveUsage[statName].toString()}>
      <li className={className + ' slave-usage-details'}>
        {humanizeStat(statName)}
      </li>
    </CopyToClipboard>
  );

  const isSlaveCritical = () => {
    return isStatCritical(props.cpusUsedStat) ||
           isStatCritical(props.memoryBytesUsedStat) ||
           isStatCritical(props.numTasksStat);
  };

  const isSlaveWarning = () => {
    return isStatWarning(props.cpusUsedStat) ||
           isStatWarning(props.memoryBytesUsedStat) ||
           isStatWarning(props.numTasksStat);
  };

  const isStatCritical = (statName) => {
    switch (statName) {
      case props.cpusUsedStat:
        return (props.slaveUsage.cpusUsed / getMaxAvailableResource(statName)) > props.cpusCriticalThreshold;
      case props.memoryBytesUsedStat:
        // todo: create util method to convert from mb to bytes
        return (props.slaveUsage.memoryBytesUsed / (getMaxAvailableResource(statName) * Math.pow(1024, 2))) > props.memoryCriticalThreshold;
      case props.numTasksStat:
        return props.slaveUsage.numTasks > props.numTasksWarning;
    }
  };

  const isStatWarning = (statName) => {
    switch (statName) {
      case props.cpusUsedStat:
        return (props.slaveUsage.cpusUsed / getMaxAvailableResource(statName)) > props.cpusWarningThreshold;
      case props.memoryBytesUsedStat:
        // todo: create util method to convert from mb to bytes
        return (props.slaveUsage.memoryBytesUsed / (getMaxAvailableResource(statName) * Math.pow(1024, 2))) > props.memoryWarningThreshold;
      case props.numTasksStat:
        return props.slaveUsage.numTasks > props.numTasksWarning;
    }
  };

  const getMaxAvailableResource = (statName) => {
    switch (statName) {
      case props.cpusUsedStat:
        try {
          return parseFloat(props.slaveInfo.attributes.real_cpus) || props.slaveInfo.resources.cpus;
        } catch (e) {
          throw Utils.formatUnicorn('Could not find resource (cpus) for slave {host} ({id})', props.slaveInfo);
        }
      case props.memoryBytesUsedStat:
        try {
          return parseFloat(props.slaveInfo.attributes.real_memory_mb) || props.slaveInfo.resources.mem;
        } catch (e) {
          throw Utils.formatUnicorn('Could not find resources (memory) for slave {host} ({id})', props.slaveInfo);
        }
      default:
        throw Utils.formatUnicorn('{0} is an unsupported statistic', statName);
    }
  };

  const humanizeStat = (statName) => {
    switch (statName) {
      case props.memoryBytesUsedStat:
        return Utils.formatUnicorn('Memory used : {0}', Utils.humanizeFileSize(props.slaveUsage[statName]));
      case props.timestampStat:
        return Utils.formatUnicorn('{0} : {1}', Utils.humanizeCamelcase(statName), Utils.absoluteTimestampWithSeconds(props.slaveUsage[statName]));
      case props.slaveIdStat:
        return Utils.formatUnicorn('Host : {host}', props.slaveInfo);
      default:
        return Utils.formatUnicorn('{0} : {1}', Utils.humanizeCamelcase(statName), props.slaveUsage[statName]);
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
  index : PropTypes.number.isRequired,
  cpusWarningThreshold : PropTypes.number,
  cpusCriticalThreshold : PropTypes.number,
  memoryWarningThreshold : PropTypes.number,
  memoryCriticalThreshold : PropTypes.number,
  numTasksWarning : PropTypes.number,
  numTasksCritical : PropTypes.number,
  cpusUsedStat : PropTypes.string,
  memoryBytesUsedStat : PropTypes.string,
  numTasksStat : PropTypes.string,
  slaveIdStat : PropTypes.string,
  timestampStat : PropTypes.string
};


function mapStateToProps() {
  return {
    cpusWarningThreshold : .80,
    cpusCriticalThreshold : .90,
    memoryWarningThreshold : .80,
    memoryCriticalThreshold : .90,
    numTasksWarning : 100,
    numTasksCritical : 120,
    cpusUsedStat : 'cpusUsed',
    memoryBytesUsedStat : 'memoryBytesUsed',
    numTasksStat : 'numTasks',
    slaveIdStat : 'slaveId',
    timestampStat : 'timestamp'
  };
}

export default connect(mapStateToProps)(SlaveUsage);
