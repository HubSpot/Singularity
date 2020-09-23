import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../../rootComponent';
import { FetchAgentUsages, FetchAgents } from '../../../actions/api/agents';
import { FetchSingularityStatus } from '../../../actions/api/state';
import { FetchUtilization } from '../../../actions/api/utilization';
import { STAT_NAMES, WHOLE_NUMBER, HEALTH_SCALE_MAX } from '../Constants';
import Utils from '../../../utils';
import ResourceHealthData from './ResourceHealthData';
import AgentAggregates from './AgentAggregates';
import ClusterAggregates from './ClusterAggregates';

const getAgentInfo = (agents, agentUsage) => {
  return _.findWhere(agents, {'id': agentUsage.agentId});
};

const getUtilizationData = (agents, agentUsages) => {
  return agentUsages.map((agentUsage) => {
    const agentInfo = getAgentInfo(agents, agentUsage);

    const totalCpuResource = Utils.getMaxAvailableResource(agentInfo, STAT_NAMES.cpusUsedStat);
    const cpuUtilized = Utils.roundTo((agentUsage[STAT_NAMES.cpusUsedStat] / totalCpuResource) * HEALTH_SCALE_MAX, WHOLE_NUMBER);

    const totalMemoryResource = Utils.getMaxAvailableResource(agentInfo, STAT_NAMES.memoryBytesUsedStat);
    const memoryUtilized = Utils.roundTo((agentUsage[STAT_NAMES.memoryBytesUsedStat] / totalMemoryResource) * HEALTH_SCALE_MAX, WHOLE_NUMBER);

    const totalDiskResource = Utils.getMaxAvailableResource(agentInfo, STAT_NAMES.diskBytesUsedStat);
    const diskUtilized = Utils.roundTo((agentUsage[STAT_NAMES.diskBytesUsedStat] / totalDiskResource) * HEALTH_SCALE_MAX, WHOLE_NUMBER);

    return {agentInfo, agentUsage, totalCpuResource, cpuUtilized, totalMemoryResource, memoryUtilized, totalDiskResource, diskUtilized};
  });
};

const AgentUsage = ({agents, agentUsages, activeTasks, clusterUtilization, totalRequests}) => {
  const activeAgents = agents.filter(Utils.isActiveAgent);
  const utilizationData = getUtilizationData(activeAgents, agentUsages);

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
    <div id="agent-usage-page">
      <h1>Agent Usage</h1>
      <div>
        <AgentAggregates agents={activeAgents} agentUsages={agentUsages} activeTasks={activeTasks} utilization={clusterUtilization} />
      </div>
      <div id="agent-health">
        <h3>Agent health</h3>
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

AgentUsage.propTypes = {
  agentUsages: PropTypes.arrayOf(PropTypes.object),
  agents: PropTypes.arrayOf(PropTypes.object),
  activeTasks: PropTypes.number
};

function mapStateToProps(state) {
  return {
    agentUsages: state.api.agentUsages.data,
    agents: state.api.agents.data,
    activeTasks: state.api.status.data.activeTasks,
    clusterUtilization: state.api.utilization.data,
    totalRequests: state.api.requests.data.length
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchAgents: () => dispatch(FetchAgents.trigger()),
    fetchAgentUsages: () => dispatch(FetchAgentUsages.trigger()),
    fetchSingularityStatus: () => dispatch(FetchSingularityStatus.trigger())
  };
}

const refresh = () => (dispatch) =>
  Promise.all([
    dispatch(FetchAgents.trigger()),
    dispatch(FetchAgentUsages.trigger()),
    dispatch(FetchSingularityStatus.trigger()),
    dispatch(FetchUtilization.trigger())
  ]);

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(AgentUsage, refresh, true, true));
