import React, { PropTypes } from 'react';
import Utils from '../../../utils';
import AgentResourceHealthMenuItems from './AgentResourceHealthMenuItems';
import { Button, OverlayTrigger, Popover } from 'react-bootstrap';
import { HEALTH_SCALE, STAT_NAMES } from '../Constants';

const AgentResourceHealth = ({agentInfo, agentUsage, resource, totalResource, utilization}) => {
  const checkStats = (val, stat) => {
    if (Utils.isResourceStat(stat) && stat !== resource) {
      return null;
    }

    const newStat = {
      name : stat,
      value : (stat === STAT_NAMES.agentIdStat ? agentInfo.host : val),
    };

    if (Utils.isResourceStat(stat)) {
      newStat.maybeTotalResource = totalResource;
    }

    return newStat;
  };

  const checkedStats = _.map(agentUsage, checkStats).filter(obj => obj);

  const popover = (
    <Popover id={agentUsage.agentId} className="agent-usage-popover" title={Utils.humanizeAgentHostName(agentInfo.host, true)} >
      <AgentResourceHealthMenuItems stats={checkedStats} />
    </Popover>
  );

  return (
    <OverlayTrigger trigger="click" placement="bottom" overlay={popover} rootClose={true}>
      <Button className="single-agent-btn" style={{backgroundColor : HEALTH_SCALE[utilization]}} />
    </OverlayTrigger>
  );
};

AgentResourceHealth.propTypes = {
  agentUsage : PropTypes.shape({
    agentId : PropTypes.string.isRequired,
    cpusUsed : PropTypes.number.isRequired,
    memoryBytesUsed : PropTypes.number.isRequired,
    numTasks : PropTypes.number.isRequired,
    timestamp : PropTypes.number.isRequired
  }),
  agentInfo : PropTypes.shape({
    host : PropTypes.string.isRequired,
    attributes : PropTypes.object.isRequired,
    resources : PropTypes.object
  }),
  resource : PropTypes.string.isRequired,
  totalResource : PropTypes.number.isRequired,
  utilization : PropTypes.number.isRequired
};

export default AgentResourceHealth;
