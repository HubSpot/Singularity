import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../../rootComponent';
import { FetchSlaveUsages, FetchSlaves } from '../../../actions/api/slaves';
import { FetchSingularityStatus } from '../../../actions/api/state';
import { STAT_NAMES, WHOLE_NUMBER, HEALTH_SCALE_MAX } from '../Constants';
import Utils from '../../../utils';
import ResourceHealthData from './ResourceHealthData';
import SlaveAggregates from './SlaveAggregates';
import ClusterAggregates from './ClusterAggregates';

const getSlaveInfo = (slaves, slaveUsage) => {
  return _.findWhere(slaves, {'id': slaveUsage.slaveId});
};

const getUtilizationData = (slaves, slaveUsages) => {
  return slaveUsages.map((slaveUsage) => {
    const slaveInfo = getSlaveInfo(slaves, slaveUsage);

    const totalCpuResource = Utils.getMaxAvailableResource(slaveInfo, STAT_NAMES.cpusUsedStat);
    const cpuUtilized = Utils.roundTo((slaveUsage[STAT_NAMES.cpusUsedStat] / totalCpuResource) * HEALTH_SCALE_MAX, WHOLE_NUMBER);

    const totalMemoryResource = Utils.getMaxAvailableResource(slaveInfo, STAT_NAMES.memoryBytesUsedStat);
    const memoryUtilized = Utils.roundTo((slaveUsage[STAT_NAMES.memoryBytesUsedStat] / totalMemoryResource) * HEALTH_SCALE_MAX, WHOLE_NUMBER);

    const totalDiskResource = Utils.getMaxAvailableResource(slaveInfo, STAT_NAMES.diskBytesUsedStat);
    const diskUtilized = Utils.roundTo((slaveUsage[STAT_NAMES.diskBytesUsedStat] / totalDiskResource) * HEALTH_SCALE_MAX, WHOLE_NUMBER);

    return {slaveInfo, slaveUsage, totalCpuResource, cpuUtilized, totalMemoryResource, memoryUtilized, totalDiskResource, diskUtilized};
  });
};

const SlaveUsage = ({slaves, slaveUsages, activeTasks, clusterUtilization, totalRequests}) => {
  const activeSlaves = slaves.filter(Utils.isActiveSlave);
  const utilizationData = getUtilizationData(activeSlaves, slaveUsages);

  const cpuHealthData = utilizationData.sort((a, b) => a.cpuUtilized - b.cpuUtilized).map((data, index) => {
    return <ResourceHealthData key={index} utilizationData={data} statName={STAT_NAMES.cpusUsedStat} />;
  });

  const memoryHealthData = utilizationData.sort((a, b) => a.memoryUtilized - b.memoryUtilized).map((data, index) => {
    return <ResourceHealthData key={index} utilizationData={data} statName={STAT_NAMES.memoryBytesUsedStat} />;
  });

  const diskHealthData = utilizationData.sort((a, b) => a.diskUtilized - b.diskUtilized).map((data, index) => {
    return <ResourceHealthData key={index} utilizationData={data} statName={STAT_NAMES.diskBytesUsedStat} />;
  });

  return (
    <div id="slave-usage-page">
      <h1>Slave Usage</h1>
      <div>
        <SlaveAggregates slaves={activeSlaves} slaveUsages={slaveUsages} activeTasks={activeTasks} utilization={clusterUtilization} />
      </div>
      <div id="slave-health">
        <h3>Slave health</h3>
        <h4>CPU</h4>
        <div className="cpu-health">
          {cpuHealthData}
        </div>
        <h4>Memory</h4>
        <div className="memory-health">
          {memoryHealthData}
        </div>
        <h4>Disk</h4>
        <div className="disk-health">
          {diskHealthData}
        </div>
      </div>
      <hr />
      <h1>Cluster Utilization</h1>
      <small className="last-updated">Last updated {Utils.timestampFromNow(clusterUtilization.timestamp)}</small>
      <div>
        <ClusterAggregates utilization={clusterUtilization} totalRequests={totalRequests} />
      </div>
    </div>
  );
};

SlaveUsage.propTypes = {
  slaveUsages: PropTypes.arrayOf(PropTypes.object),
  slaves: PropTypes.arrayOf(PropTypes.object),
  activeTasks: PropTypes.number
};

function mapStateToProps(state) {
  return {
    slaveUsages: state.api.slaveUsages.data,
    slaves: state.api.slaves.data,
    activeTasks: state.api.status.data.activeTasks,
    clusterUtilization: state.api.utilization.data,
    totalRequests: state.api.requests.data.length
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchSlaves: () => dispatch(FetchSlaves.trigger()),
    fetchSlaveUsages: () => dispatch(FetchSlaveUsages.trigger()),
    fetchSingularityStatus: () => dispatch(FetchSingularityStatus.trigger())
  };
}

const refresh = () => (dispatch) =>
  Promise.all([
    dispatch(FetchSlaves.trigger()),
    dispatch(FetchSlaveUsages.trigger()),
    dispatch(FetchSingularityStatus.trigger())
  ]);

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(SlaveUsage, refresh, true, true));
