import React, { PropTypes } from 'react';
import {DropdownButton, MenuItem, Dropdown, ButtonGroup, Button, Nav, NavItem} from 'react-bootstrap';
import {Glyphicon} from 'react-bootstrap';
import Utils from '../../utils';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { Link } from 'react-router';
import CopyToClipboard from 'react-copy-to-clipboard';

const SlaveUsage = (props) => {
	// console.log(props.slaves);

	const navigation = (
		// TODO: fix justified
		<Nav bsStyle='pills' activeKey={2} justified={true}>
			<NavItem eventKey={1} title='aggregate'> Aggregate </NavItem>
			<NavItem eventKey={2} title='heatmap'> Heatmap </NavItem>
		</Nav>
	);

	const drawSlaves = (slaves) => {
		console.log('drawSlaves() G, Ox3, R, Gx5');
		
		var grid = slaves.map((slave, index) => {
			return drawSlave(slave, index);
		});
		return grid
	};

	const drawSlave = (slave, index) => {
    var criticalGlyph = 'glyphicon glyphicon-remove-sign'
    var warningGlyph = 'glyphicon glyphicon-minus-sign'
    var okGlyph = 'glyphicon glyphicon-ok-sign'
    var criticalStyle = 'danger'
    var warningStyle = 'warning'
    var okStyle = 'success'

		if (slave.cpusUsed > props.cpusUsedCritical || slave.numTasks > props.numTasksCritical) {
			return slaveWithStats(slave, index, criticalStyle, criticalGlyph);
		} else if (slave.cpusUsed > props.cpusUsedWarning || slave.numTasks > props.numTasksWarning) {
			return slaveWithStats(slave, index, warningStyle, warningGlyph);
		} else {
			return slaveWithStats(slave, index, okStyle, okGlyph);
		}
	};

  const slaveWithStats = (slave, index, bsStyle, glyphicon) => (
    <Dropdown id={index.toString()}>
      <Dropdown.Toggle bsSize='large' bsStyle={bsStyle} noCaret={true}>
        <Glyphicon glyph={glyphicon} />
      </Dropdown.Toggle>
      <Dropdown.Menu>
        {checkSlaveStats(slave)}
      </Dropdown.Menu>
    </Dropdown>
  );

  const checkSlaveStats = (slave) => {
    return Object.keys(slave).map((slaveStat) => {
      return checkSlaveStat(slaveStat, slave[slaveStat]);
    });
  }

  const checkSlaveStat = (statName, statValue) => {
    var critical = 'color-error';
    var warning = 'color-warning';

    switch (statName) {
      case 'cpusUsed':
        if (statValue > props.cpusUsedCritical) {
          return slaveStat(statName, statValue, critical);
        } else if (statValue > props.cpusUsedWarning) {
          return slaveStat(statName, statValue, warning);
        }
        break;
      case 'numTasks':
        if (statValue > props.numTasksCritical) {
          return slaveStat(statName, statValue, critical);
        } else if (statValue > props.numTasksWarning) {
          return slaveStat(statName, statValue, warning);
        }
    }

    return slaveStat(statName, statValue, null);
  }

  const statItemStyle = {'white-space' : 'nowrap', 'padding-left' : '20px', 'padding-right' : '5px'};

  const slaveStat = (statName, statValue, className) => (
    <li 
      className={className}
      style={statItemStyle}>
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
				{drawSlaves(props.slaves)}
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
