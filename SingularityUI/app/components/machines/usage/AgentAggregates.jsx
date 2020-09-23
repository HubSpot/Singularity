import React, { PropTypes } from 'react';
import Utils from '../../../utils';
import { STAT_NAMES } from '../Constants';
import LabeledColumn from './LabeledColumn';
import Aggregate from './Aggregate';

const getPctAgentUsage = (usageMapper, resourceMapper) => (agents, agentUsages) => {
  const totalUsage = agentUsages.map(usageMapper)
    .reduce((acc, val) => acc + parseFloat(val), 0);

  const totalResource = agents.map(resourceMapper)
    .reduce((acc, val) => acc + parseFloat(val), 0);

  return Utils.toDisplayPercentage(totalUsage, totalResource);
};

const getCpuUtilizationPct = getPctAgentUsage(
  usage => usage.cpusUsed,
  agent => Utils.getMaxAvailableResource(agent, STAT_NAMES.cpusUsedStat)
);

const getMemUtilizationPct = getPctAgentUsage(
  usage => usage.memoryBytesUsed,
  agent => Utils.getMaxAvailableResource(agent, STAT_NAMES.memoryBytesUsedStat)
);

const getDiskUtilizationPct = getPctAgentUsage(
  usage => usage.diskBytesUsed,
  agent => Utils.getMaxAvailableResource(agent, STAT_NAMES.diskBytesUsedStat)
);

const AgentAggregates = ({agents, agentUsages, activeTasks, utilization}) => {
  return (
    <div>
      <div className="agent-aggregates row">
        <Aggregate width={2} value={agents.length} label="Active Agents" />
        <Aggregate width={2} value={activeTasks} label="Tasks Running" />
      </div>
      <div className="agent-aggregates row">
        <LabeledColumn title="Current" width={6}>
          <Aggregate width={2} value={getCpuUtilizationPct(agents, agentUsages)} graph={true} label="CPU" />
          <Aggregate width={2} value={getMemUtilizationPct(agents, agentUsages)} graph={true} label="Memory" />
          <Aggregate width={2} value={getDiskUtilizationPct(agents, agentUsages)} graph={true} label="Disk" />
        </LabeledColumn>

        <LabeledColumn className="info" title="24-Hour Average" width={6}>
          <Aggregate width={2} value={Utils.toDisplayPercentage(utilization.totalCpuUsed, utilization.totalCpuAvailable)} graph={true} label="CPU" />
          <Aggregate width={2} value={Utils.toDisplayPercentage(utilization.totalMemBytesUsed, utilization.totalMemBytesAvailable)} graph={true} label="Memory" />
          <Aggregate width={2} value={Utils.toDisplayPercentage(utilization.totalDiskBytesUsed, utilization.totalDiskBytesAvailable)} graph={true} label="Disk" />
        </LabeledColumn>
      </div>
    </div>
  );
};

AgentAggregates.propTypes = {
  agents: PropTypes.array,
  agentUsages: PropTypes.array,
  activeTasks: PropTypes.number.isRequired,
  utilization: PropTypes.object
};

export default AgentAggregates;
