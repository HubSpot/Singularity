import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { FetchSlaveUsages, FetchSlaves } from '../../actions/api/slaves';
import { FetchSingularityStatus } from '../../actions/api/state';
import Utils from '../../utils';
import SlaveAggregates from './SlaveAggregates';
import SlaveHealth from './SlaveHealth';

const getSlaveInfo = (slaves, slaveUsage) => {
  return _.findWhere(slaves, {'id' : slaveUsage.slaveId});
};

const SlaveUsage = ({slaves, slaveUsages, activeTasks}) => {
  const activeSlaves = slaves.filter(Utils.isActiveSlave);
  const activeSlaveUsages = slaveUsages.filter((slaveUsage) => getSlaveInfo(activeSlaves, slaveUsage));

  const slaveHealthData = activeSlaveUsages.map((slaveUsage, index) => {
    const slaveInfo = getSlaveInfo(activeSlaves, slaveUsage);
    return <SlaveHealth key={index} slaveUsage={slaveUsage} slaveInfo={slaveInfo} />;
  });

  return (
    <div id="slave-usage-page">
      <h1>Slave Usage</h1>
      <div>
        <SlaveAggregates slaves={activeSlaves} slaveUsages={activeSlaveUsages} activeTasks={activeTasks} />
      </div>
      <hr />
      <div id="slave-health">
        <h3>Slave health</h3>
        {slaveHealthData}
      </div>
    </div>
  );
};

SlaveUsage.propTypes = {
  slaveUsages : PropTypes.arrayOf(PropTypes.object),
  slaves : PropTypes.arrayOf(PropTypes.object),
  activeTasks : PropTypes.number
};

function mapStateToProps(state) {
  return {
    slaveUsages : state.api.slaveUsages.data,
    slaves : state.api.slaves.data,
    activeTasks : state.api.status.data.activeTasks
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchSlaves : () => dispatch(FetchSlaves.trigger()),
    fetchSlaveUsages : () => dispatch(FetchSlaveUsages.trigger()),
    fetchSingularityStatus : () => dispatch(FetchSingularityStatus.trigger())
  };
}

const refresh = () => (dispatch) =>
  Promise.all([
    dispatch(FetchSlaves.trigger()),
    dispatch(FetchSlaveUsages.trigger()),
    dispatch(FetchSingularityStatus.trigger())
  ]);

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(SlaveUsage, refresh, true, true));
