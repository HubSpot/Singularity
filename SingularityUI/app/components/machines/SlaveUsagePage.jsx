import React, { PropTypes } from 'react';
import { Nav, NavItem } from 'react-bootstrap';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { FetchSlaveUsages, FetchSlaves } from '../../actions/api/slaves';
import SlaveUsage from './SlaveUsage';

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
      <h1>Slave Usage</h1>
      <hr />
      <div id="slaves">
        {slaveUsageData}
      </div>
    </div>
  );
};


SlaveUsage.propTypes = {
  slaveUsages : PropTypes.arrayOf(PropTypes.object),
  slaves : PropTypes.arrayOf(PropTypes.object)
};

function mapStateToProps(state) {
  return {
    slaveUsages : state.api.slaveUsages.data,
    slaves : state.api.slaves.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchSlaves : () => dispatch(FetchSlaves.trigger()),
    fetchSlaveUsages : () => dispatch(FetchSlaveUsages.trigger())
  };
}

const refresh = () => (dispatch) =>
  Promise.all([
    dispatch(FetchSlaves.trigger()),
    dispatch(FetchSlaveUsages.trigger())
  ]);

const initialize = () => (dispatch) =>
  Promise.all([]).then(() => dispatch(refresh()));


export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(SlaveUsagePage, refresh, true, true, initialize));
