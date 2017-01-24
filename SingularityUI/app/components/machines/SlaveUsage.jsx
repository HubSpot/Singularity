import React, { PropTypes } from 'react';
import {DropdownButton, MenuItem, Dropdown, ButtonGroup, Button, Nav, NavItem} from 'react-bootstrap';
import {Glyphicon} from 'react-bootstrap';
import Utils from '../../utils';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { Link } from 'react-router';
import CopyToClipboard from 'react-copy-to-clipboard';

// TODO:
// put test data in a global variable

// loop through test data
// for each slave
	// draw a small green cube

	// heat map type coloring, warning levels and critical levels should be set
	// if cpusUsed > 5, color it light red
	// if cpusUsed > 10, color it red
	// if numTasks > 10, color it light red
	// if numTasks > 20, color it red

	// on click show details of slave

const SlaveUsage = (props) => {
	// console.log(props.slaves);

	const navigation = (
		// TODO: fix justified
		<Nav bsStyle='pills' activeKey={2} justified={true}>
			<NavItem eventKey={1} title='aggregate'> Aggregate </NavItem>
			<NavItem eventKey={2} title='heatmap'> Heatmap </NavItem>
		</Nav>
	);

	const drawUsage = (slaves) => {
		console.log('drawUsage() G, Ox3, R, Gx5');
		
		var grid = slaves.map((slave, index) => {
			return checkSlaveStatus(slave, index);
		});
		return grid
	};

	const checkSlaveStatus = (slave, index) => {
		if (slave.cpusUsed > props.cpusUsedCritical || slave.numTasks > props.numTasksCritical) {
			return criticalSlave(slave, index);
		} else if (slave.cpusUsed > props.cpusUsedWarning || slave.numTasks > props.numTasksWarning) {
			return warningSlave(slave, index);
		} else {
			return okSlave(slave, index);
		}
	};

	const okSlave = (slave, index) => (
    <Dropdown id={index.toString()}>
      <Dropdown.Toggle bsSize='large' bsStyle='success' noCaret={true}>
        <Glyphicon glyph='glyphicon glyphicon-ok-sign' />
      </Dropdown.Toggle>
      <Dropdown.Menu>
        {getSlaveStats(slave)}
      </Dropdown.Menu>
    </Dropdown>
  );

  const warningSlave = (slave, index) => (
    <Dropdown id={index.toString()}>
      <Dropdown.Toggle bsSize='large' bsStyle='warning' noCaret={true}>
        <Glyphicon glyph='gglyphicon glyphicon-minus-sign' />
      </Dropdown.Toggle>
      <Dropdown.Menu>
        {getSlaveStats(slave)}
      </Dropdown.Menu>
    </Dropdown>
  );

	const criticalSlave = (slave, index) => (
		<Dropdown id={index.toString()}>
      <Dropdown.Toggle bsSize='large' bsStyle='danger' noCaret={true}>
        <Glyphicon glyph='gglyphicon glyphicon-remove-sign' />
      </Dropdown.Toggle>
      <Dropdown.Menu>
        {getSlaveStats(slave)}
      </Dropdown.Menu>
    </Dropdown>
	);

  const getSlaveStats = (slave) => {
    return Object.keys(slave).map((slaveStat) => {
      return drawSlaveStat(slaveStat, slave[slaveStat]);
    });
  }

  const drawSlaveStat = (statName, statValue) => {
    switch (statName) {
      case 'cpusUsed':
        if (statValue > props.cpusUsedCritical) {
          return criticalStat(statName, statValue);
        } else if (statValue > props.cpusUsedWarning) {
          return warningStat(statName, statValue);
        }
        break;
      case 'numTasks':
        if (statValue > props.numTasksCritical) {
          return criticalStat(statName, statValue);
        } else if (statValue > props.numTasksWarning) {
          return warningStat(statName, statValue);
        }
    }

    return okStat(statName, statValue);
  }

  const statItemStyle = {'white-space' : 'nowrap', 'padding-left' : '20px', 'padding-right' : '5px'};

  const criticalStat = (statName, statValue) => (
    <li 
      className='color-error' 
      style={statItemStyle}>
      {statName} : {statValue}
    </li>
  )

  const warningStat = (statName, statValue) => (
    <li 
      className='color-warning' 
      style={statItemStyle}>
      {statName} : {statValue}
    </li>
  )

  const okStat = (statName, statValue) => (
    <li style={statItemStyle}>
      {statName} : {statValue}
    </li>
  )

	return (
		<div>
			<div id='nav'>
				{navigation}
			</div>
      <hr/>
			<div id='slaves'>
				{drawUsage(props.slaves)}
			</div>
		</div>
	);
};



SlaveUsage.propTypes = {
	slaves : PropTypes.arrayOf(PropTypes.shape({
		state : PropTypes.string
	})),
	cpusUsedWarning : PropTypes.number,
	cpusUsedCritical : PropTypes.number,
	numTasksWarning : PropTypes.number,
	numTasksCritical : PropTypes.number
};



function mapStateToProps(state) {
	return {
		slaves : flatten(Array(10).fill(TEST_DATA)),
		cpusUsedWarning : 5,
		cpusUsedCritical : 10,
		numTasksWarning : 10,
		numTasksCritical : 20
	};
}

function initialize(props) {
	return Promise.all([]);
}

function refresh(props) {
	return Promise.all([]);
}

// TODO: mappings
export default connect(mapStateToProps, null)(rootComponent(SlaveUsage, 'SlaveUsage', refresh, true, true, initialize));

const flatten = arr => arr.reduce((a, b) => a.concat(Array.isArray(b) ? flatten(b) : b), []);
const TEST_DATA = [
    {
        "cpusUsed": 1,
        "memoryBytesUsed": 1318215680,
        "numTasks": 2,
        "slaveId": "dd4fdc07-b166-43f6-97de-c331473a3a71-S105",
        "timestamp": 1484773218213
    },
    {
        "cpusUsed": 6,
        "memoryBytesUsed": 2788343808,
        "numTasks": 6,
        "slaveId": "dd4fdc07-b166-43f6-97de-c331473a3a71-S101",
        "timestamp": 1484773218213
    },
    {
        "cpusUsed": 10,
        "memoryBytesUsed": 817430528,
        "numTasks": 2,
        "slaveId": "dd4fdc07-b166-43f6-97de-c331473a3a71-S109",
        "timestamp": 1484773218213
    },
    {
        "cpusUsed": 2,
        "memoryBytesUsed": 817430528,
        "numTasks": 12,
        "slaveId": "dd4fdc07-b166-43f6-97de-c331473a3a71-S109",
        "timestamp": 1484773218213
    },
    {
        "cpusUsed": 2,
        "memoryBytesUsed": 817430528,
        "numTasks": 22,
        "slaveId": "dd4fdc07-b166-43f6-97de-c331473a3a71-S109",
        "timestamp": 1484773218213
    },
    {
        "cpusUsed": 5,
        "memoryBytesUsed": 817430528,
        "numTasks": 2,
        "slaveId": "dd4fdc07-b166-43f6-97de-c331473a3a71-S109",
        "timestamp": 1484773218213
    },
    {
        "cpusUsed": 1,
        "memoryBytesUsed": 1318215680,
        "numTasks": 2,
        "slaveId": "dd4fdc07-b166-43f6-97de-c331473a3a71-S105",
        "timestamp": 1484773218213
    },
    {
        "cpusUsed": 1,
        "memoryBytesUsed": 1318215680,
        "numTasks": 2,
        "slaveId": "dd4fdc07-b166-43f6-97de-c331473a3a71-S105",
        "timestamp": 1484773218213
    },
    {
        "cpusUsed": 1,
        "memoryBytesUsed": 1318215680,
        "numTasks": 2,
        "slaveId": "dd4fdc07-b166-43f6-97de-c331473a3a71-S105",
        "timestamp": 1484773218213
    },
    {
        "cpusUsed": 1,
        "memoryBytesUsed": 1318215680,
        "numTasks": 2,
        "slaveId": "dd4fdc07-b166-43f6-97de-c331473a3a71-S105",
        "timestamp": 1484773218213
    }
];
