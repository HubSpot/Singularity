import React, { PropTypes } from 'react';
import { Nav, NavItem } from 'react-bootstrap';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { FetchSlaveUsages, FetchSlaves } from '../../actions/api/slaves';
import SlaveUsage from './SlaveUsage';

const navigation = (
  <Nav bsStyle="pills" activeKey={2} justified={true}>
    <NavItem eventKey={1} title="aggregate"> Aggregate </NavItem>
    <NavItem eventKey={2} title="heatmap"> Heatmap </NavItem>
  </Nav>
);

const getSlaveInfo = (slaves, slaveUsage) => {
  return _.findWhere(slaves, {'id' : slaveUsage.slaveId});
};

const SlaveUsagePage = ({slaves, slaveUsages}) => {
  const slaveUsageData = slaveUsages.map((slaveUsage, index) => {
    const slaveInfo = getSlaveInfo(slaves, slaveUsage);
    return <SlaveUsage key={index} slaveUsage={slaveUsage} slaveInfo={slaveInfo} />;
  });

  return (
    <div>
      <div id="nav">
        {navigation}
      </div>
      <hr />
      <div id="slaves">
        {slaveUsageData}
      </div>
    </div>
  );
};


SlaveUsage.propTypes = {
  slaveUsages : PropTypes.arrayOf(PropTypes.object),
  slaves : PropTypes.arrayOf(PropTypes.object),
  fetchSlaveUsages : React.PropTypes.func
};

function mapStateToProps(state) {
  return {
    slaveUsages : state.api.slaveUsages.data,
    slaves : state.api.slaves.data
  };
}

const mapDispatchToProps = (dispatch) => ({
  fetchSlaveUsages : () => dispatch(FetchSlaveUsages.trigger()),
  fetchSlaves : () => dispatch(FetchSlaves.trigger())
});

function initialize(props) {
  return Promise.all([
    props.fetchSlaveUsages(),
    props.fetchSlaves()
  ]);
}

function refresh(props) {
  return Promise.all([
    props.fetchSlaveUsages(),
    props.fetchSlaves()
  ]);
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(SlaveUsagePage, 'SlaveUsagePage', refresh, true, true, initialize));
