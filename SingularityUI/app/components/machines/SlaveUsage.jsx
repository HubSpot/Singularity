import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { FetchSlaveUsages, FetchSlaves } from '../../actions/api/slaves';
import SlaveHealth from './SlaveHealth';

const getSlaveInfo = (slaves, slaveUsage) => {
  return _.findWhere(slaves, {'id' : slaveUsage.slaveId});
};

const SlaveUsage = ({slaves, slaveUsages}) => {
  const slaveHealthData = slaveUsages.map((slaveUsage, index) => {
    const slaveInfo = getSlaveInfo(slaves, slaveUsage);
    return <SlaveHealth key={index} slaveUsage={slaveUsage} slaveInfo={slaveInfo} />;
  });

  return (
    <div id="slave-usage-page">
      <h1>Slave Usage</h1>
      <div>
        Aggregate data will appear here
      </div>

      <div id="slave-health">
        <h3>Slave health</h3>
        {slaveHealthData}
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


export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(SlaveUsage, refresh, true, true, initialize));
