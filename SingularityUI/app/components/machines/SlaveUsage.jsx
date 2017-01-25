import React, { PropTypes } from 'react';
import { Dropdown, Nav, NavItem } from 'react-bootstrap';
import { Glyphicon } from 'react-bootstrap';
import Utils from '../../utils';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { FetchSlaveUsage, FetchSlaves } from '../../actions/api/slaves';
import CopyToClipboard from 'react-copy-to-clipboard';

const SlaveUsage = (props) => {
  const navigation = (
    // TODO: fix justified
    <Nav bsStyle='pills' activeKey={2} justified={true}>
      <NavItem eventKey={1} title='aggregate'> Aggregate </NavItem>
      <NavItem eventKey={2} title='heatmap'> Heatmap </NavItem>
    </Nav>
  );

  const drawSlaves = () => {    
    var grid = props.slaveUsage.map((slave, index) => {
      return drawSlave(slave, index);
    });
    return grid;
  };

  const drawSlave = (slave, index) => {
    var criticalGlyph = 'glyphicon glyphicon-remove-sign';
    var warningGlyph = 'glyphicon glyphicon-minus-sign';
    var okGlyph = 'glyphicon glyphicon-ok-sign';
    var criticalStyle = 'danger';
    var warningStyle = 'warning';
    var okStyle = 'success';

    if (isStatCritical(slave, props.cpusUsedStat) || isStatCritical(slave, props.memoryBytesUsedStat) || isStatCritical(slave, props.numTasksStat)) {
      return slaveWithStats(slave, index, criticalStyle, criticalGlyph);
    } else if (isStatWarning(slave, props.cpusUsedStat) || isStatWarning(slave, props.memoryBytesUsedStat) || isStatWarning(slave, props.numTasksStat)) {
      return slaveWithStats(slave, index, warningStyle, warningGlyph);
    } else {
      return slaveWithStats(slave, index, okStyle, okGlyph);
    }
  };

  const isStatWarning = (slave, statName) => {
    switch (statName) {
      case props.cpusUsedStat:
        return (slave.cpusUsed / getMaxAvailableResource(slave, statName)) > props.cpusWarningThreshold;
      case props.memoryBytesUsedStat:
        // todo: create util method to convert from mb to bytes
        return (slave.memoryBytesUsed / (getMaxAvailableResource(slave, statName) * Math.pow(1024, 2))) > props.memoryWarningThreshold;
      case props.numTasksStat:
        return slave.numTasks > props.numTasksWarning;
    }
  };

  const isStatCritical = (slave, statName) => {
    switch (statName) {
      case props.cpusUsedStat:
        return (slave.cpusUsed / getMaxAvailableResource(slave, statName)) > props.cpusCriticalThreshold;
      case props.memoryBytesUsedStat:
        // todo: create util method to convert from mb to bytes
        return (slave.memoryBytesUsed / (getMaxAvailableResource(slave, statName) * Math.pow(1024, 2))) > props.memoryCriticalThreshold;
      case props.numTasksStat:
        return slave.numTasks > props.numTasksWarning;
    }
  };

  const getMaxAvailableResource = (slave, statName) => {
    var slaveInfo = getSlaveInfo(slave);
    
    switch (statName) {
      case props.cpusUsedStat:
        try {
          return parseFloat(slaveInfo.attributes.real_cpus) || slaveInfo.resources.cpus;
        } catch (e) {
          throw 'Could not find resources (cpus) for slave ' + slave.slaveId;
        }
      case props.memoryBytesUsedStat:
        try {
          return parseFloat(slaveInfo.attributes.real_memory_mb) || slaveInfo.resources.mem;
        } catch (e) {
          throw 'Could not find resources (memory) for slave ' + slave.slaveId;
        }
      default:
        throw statName + ' is an unsupported statistic';
    }
  };

  const slaveWithStats = (slave, index, bsStyle, glyphicon) => (
    <Dropdown key={slave.slaveId} id={index.toString()}>
      <Dropdown.Toggle bsSize='large' bsStyle={bsStyle} noCaret={true} className='single-slave-btn'>
        <Glyphicon glyph={glyphicon} />
      </Dropdown.Toggle>
      <Dropdown.Menu>
        {checkSlaveStats(slave, index)}
      </Dropdown.Menu>
    </Dropdown>
  );

  const checkSlaveStats = (slave, index) => {
    return Object.keys(slave).map((slaveStat) => {
      return checkSlaveStat(slave, slaveStat, index);
    });
  };

  const checkSlaveStat = (slave, statName, index) => {
    var critical = 'color-error';
    var warning = 'color-warning';

    if (isStatCritical(slave, statName)) {
      return slaveStat(slave, statName, critical, index);
    } else if (isStatWarning(slave, statName)) {
      return slaveStat(slave, statName, warning, index);
    } 

    return slaveStat(slave, statName, null, index);
  };

  const slaveStat = (slave, statName, className, index) => (
    <CopyToClipboard key={statName + index} text={slave[statName].toString()}>  
      <li className={className + ' slave-usage-details'}>
        {humanizeStat(slave, statName)}
      </li>
    </CopyToClipboard>
  );

  const humanizeStat = (slave, statName) => {
    switch (statName) {
      case props.memoryBytesUsedStat:
        return 'Memory used : ' + Utils.humanizeFileSize(slave[statName]);
      case props.timestamp:
        return Utils.humanizeCamelcase(statName) + ' : ' + Utils.absoluteTimestampWithSeconds(slave[statName]);
      case props.slaveId:
        return 'Host : ' + getSlaveInfo(slave).host;
      default:
        return Utils.humanizeCamelcase(statName) + ' : ' + slave[statName];
    }
  };

  const getSlaveInfo = (slave) => {
    return _.findWhere(props.slaves, {'id' : slave.slaveId});
  };

  return (
    <div>
      <div id='nav'>
        {navigation}
      </div>
      <hr/>
      <div id='slaves'>
        {drawSlaves(props.slaves)}
      </div>
    </div>
  );
};

SlaveUsage.propTypes = {
  slaveUsage : PropTypes.arrayOf(PropTypes.shape({
    state : PropTypes.string
  })),
  fetchSlaveUsage : React.PropTypes.func.isRequired,
  cpusWarningThreshold : PropTypes.number,
  cpusCriticalThreshold : PropTypes.number,
  memoryWarningThreshold : PropTypes.number,
  memoryCriticalThreshold : PropTypes.number,
  numTasksWarning : PropTypes.number,
  numTasksCritical : PropTypes.number,
  cpusUsedStat : PropTypes.string,
  memoryBytesUsedStat : PropTypes.string,
  numTasksStat : PropTypes.string,
  slaveId : PropTypes.string,
  timestamp : PropTypes.string
};

function mapStateToProps(state) {
  return {
    slaveUsage : state.api.slaveUsage.data,
    slaves : state.api.slaves.data,
    cpusWarningThreshold : .80,
    cpusCriticalThreshold : .90,
    memoryWarningThreshold : .80,
    memoryCriticalThreshold : .90,
    numTasksWarning : 100,
    numTasksCritical : 120,
    cpusUsedStat : 'cpusUsed',
    memoryBytesUsedStat : 'memoryBytesUsed',
    numTasksStat : 'numTasks',
    slaveId : 'slaveId',
    timestamp : 'timestamp'
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchSlaveUsage: () => dispatch(FetchSlaveUsage.trigger()),
    fetchSlaves: () => dispatch(FetchSlaves.trigger())
  };
}

function initialize(props) {
  return Promise.all([
    props.fetchSlaveUsage(),
    props.fetchSlaves()
  ]);
}

function refresh(props) {
  return Promise.all([
    props.fetchSlaveUsage(),
    props.fetchSlaves()
  ]);
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(SlaveUsage, 'SlaveUsage', refresh, true, true, initialize));
