import React, { PropTypes } from 'react';
import Utils from '../../../utils';
import AgentResourceHealth from './AgentResourceHealth';
import { OverlayTrigger, Popover } from 'react-bootstrap';
import { STAT_NAMES, HUNDREDTHS_PLACE, HEALTH_SCALE, HEALTH_SCALE_MAX } from '../Constants';

const overlayTriggers = ['hover', 'focus'];
const overlayPlacement = 'top';

const getTotalForStat = (statName, data) => {
  switch (statName) {
    case STAT_NAMES.memoryBytesUsedStat:
      return data.totalMemoryResource;
    case STAT_NAMES.cpusUsedStat:
      return data.totalCpuResource;
    case STAT_NAMES.diskBytesUsedStat:
      return data.totalDiskResource;
    default:
      throw new Error(`${name} is an unsupported statistic`);
  }
};

const getUtilizationForStat = (statName, data) => {
  switch (statName) {
    case STAT_NAMES.memoryBytesUsedStat:
      return data.memoryUtilized;
    case STAT_NAMES.cpusUsedStat:
      return data.cpuUtilized;
    case STAT_NAMES.diskBytesUsedStat:
      return data.diskUtilized;
    default:
      throw new Error(`${name} is an unsupported statistic`);
  }
};

const agentQuickStats = (data) => {
  const shortenHostName = true;
  const largeBlackCircle = '⬤';
  return (
    <Popover id="agent-usage-quick-stats-popover">
      <div className="row" id="agent-usage-quick-stats">
        <div className="col-xs-3" id="agent-name">
          {Utils.humanizeAgentHostName(data.agentInfo.host, shortenHostName)}
        </div>
        <div className="col-xs-3" id="memory-stats">
          <div id="pct-utilized">
            {Utils.roundTo(data.memoryUtilized / HEALTH_SCALE_MAX * 100, HUNDREDTHS_PLACE)}%
          </div>
          <div id="status">
            Mem <span style={{color : HEALTH_SCALE[data.memoryUtilized]}}>{largeBlackCircle}</span>
          </div>
        </div>
        <div className="col-xs-3" id="cpu-stats">
          <div id="pct-utilized">
            {Utils.roundTo(data.cpuUtilized / HEALTH_SCALE_MAX * 100, HUNDREDTHS_PLACE)}%
          </div>
          <div id="status">
            CPU <span style={{color : HEALTH_SCALE[data.cpuUtilized]}}>{largeBlackCircle}</span>
          </div>
        </div>
        <div className="col-xs-3" id="disk-stats">
          <div id="pct-utilized">
            {Utils.roundTo(data.diskUtilized / HEALTH_SCALE_MAX * 100, HUNDREDTHS_PLACE)}%
          </div>
          <div id="status">
            Disk <span style={{color : HEALTH_SCALE[data.diskUtilized]}}>{largeBlackCircle}</span>
          </div>
        </div>
      </div>
    </Popover>
  );
};

const ResourceHealthData = ({utilizationData, statName}) => {
  return (
    <OverlayTrigger trigger={overlayTriggers} overlay={agentQuickStats(utilizationData)} placement={overlayPlacement}>
      <span>
        <AgentResourceHealth
          agentUsage={utilizationData.agentUsage}
          agentInfo={utilizationData.agentInfo}
          resource={statName}
          totalResource={getTotalForStat(statName, utilizationData)}
          utilization={getUtilizationForStat(statName, utilizationData)}
        />
      </span>
    </OverlayTrigger>
  );
};

ResourceHealthData.propTypes = {
  utilizationData : PropTypes.shape({
    agentUsage : PropTypes.object.isRequired,
    agentInfo : PropTypes.object.isRequired
  }),
  statName : PropTypes.string
};

export default ResourceHealthData;
