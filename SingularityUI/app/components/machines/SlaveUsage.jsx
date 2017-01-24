import React, { PropTypes } from 'react';
import { Dropdown, Nav, NavItem } from 'react-bootstrap';
import { Glyphicon } from 'react-bootstrap';
import Utils from '../../utils';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { FetchSlaveUsage } from '../../actions/api/slaves';
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

    if (slave.cpusUsed > props.cpusUsedCritical || slave.numTasks > props.numTasksCritical) {
      return slaveWithStats(slave, index, criticalStyle, criticalGlyph);
    } else if (slave.cpusUsed > props.cpusUsedWarning || slave.numTasks > props.numTasksWarning) {
      return slaveWithStats(slave, index, warningStyle, warningGlyph);
    } else {
      return slaveWithStats(slave, index, okStyle, okGlyph);
    }
  };

  const slaveWithStats = (slave, index, bsStyle, glyphicon) => (
    <Dropdown key={slave.slaveId} id={index.toString()}>
      <Dropdown.Toggle bsSize='large' bsStyle={bsStyle} noCaret={true}>
        <Glyphicon glyph={glyphicon} />
      </Dropdown.Toggle>
      <Dropdown.Menu>
        {checkSlaveStats(slave, index)}
      </Dropdown.Menu>
    </Dropdown>
  );

  const checkSlaveStats = (slave, index) => {
    return Object.keys(slave).map((slaveStat) => {
      return checkSlaveStat(slaveStat, slave[slaveStat], index);
    });
  };

  const checkSlaveStat = (statName, statValue, index) => {
    var critical = 'color-error';
    var warning = 'color-warning';

    switch (statName) {
      case 'cpusUsed':
        if (statValue > props.cpusUsedCritical) {
          return slaveStat(statName, statValue, critical, index);
        } else if (statValue > props.cpusUsedWarning) {
          return slaveStat(statName, statValue, warning, index);
        }
        break;
      case 'numTasks':
        if (statValue > props.numTasksCritical) {
          return slaveStat(statName, statValue, critical, index);
        } else if (statValue > props.numTasksWarning) {
          return slaveStat(statName, statValue, warning, index);
        }
        break;
      case 'timestamp':
        return slaveStat(statName, Utils.absoluteTimestampWithSeconds(statValue), null, index);
    }

    return slaveStat(statName, statValue, null, index);
  };

  const statItemStyle = {'white-space' : 'nowrap', 'padding-left' : '20px', 'padding-right' : '5px'};

  const slaveStat = (statName, statValue, className, index) => (
    <li
      key={statName + index}
      className={className}
      style={statItemStyle}>
      {Utils.humanizeCamelcase(statName)} : {statValue}
    </li>
  );

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
  cpusUsedWarning : PropTypes.number,
  cpusUsedCritical : PropTypes.number,
  numTasksWarning : PropTypes.number,
  numTasksCritical : PropTypes.number
};

function mapStateToProps(state) {
  return {
    slaveUsage : state.api.slaveUsage.data,
    cpusUsedWarning : 10,
    cpusUsedCritical : 12,
    numTasksWarning : 100,
    numTasksCritical : 120
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchSlaveUsage: () => dispatch(FetchSlaveUsage.trigger())
  };
}

function initialize(props) {
  return Promise.all([
    props.fetchSlaveUsage()
  ]);
}

function refresh(props) {
  return Promise.all([
    props.fetchSlaveUsage()
  ]);
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(SlaveUsage, 'SlaveUsage', refresh, true, true, initialize));
