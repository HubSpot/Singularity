import React, {PropTypes} from 'react';
import MachinesPage from './MachinesPage';
import {Glyphicon} from 'react-bootstrap';
import FormModalButton from '../common/modal/FormModalButton';
import FormModal from '../common/modal/FormModal';
import Utils from '../../utils';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { Link } from 'react-router';
import {
  FetchAgents,
  FreezeAgent,
  DecommissionAgent,
  RemoveAgent,
  ReactivateAgent,
  FetchExpiringAgentStates,
  RemoveExpiringAgentState,
  ClearInactiveAgents
} from '../../actions/api/agents';
import { DeactivateHost, ReactivateHost, FetchInactiveHosts, ClearInactiveHosts } from '../../actions/api/inactive';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';
import { refresh, initialize } from '../../actions/ui/agents'
import CustomizeAgentsTableButton from './CustomizeAgentsTableButton';

const typeName = {
  'active': 'Activated By',
  'frozen': 'Frozen By',
  'decommissioning': 'Decommissioned By',
  'decommissioned': 'Decommissioned By'
};

const Agents = (props) => {
  const actionElements = (agent, buttonType) => {
    const elements = [];
    elements.push({
      name: 'message',
      type: FormModal.INPUT_TYPES.STRING,
      label: 'Message (optional)'
    });
    if (buttonType != 'REMOVE') {
      elements.push({
        name: 'durationMillis',
        type: FormModal.INPUT_TYPES.DURATION,
        label: 'Expiration (optional)',
        help: (
          <div>
            <p>If an expiration duration is specified, the agent will revert to the state specified below after time has elapsed.</p>
          </div>
        )
      });
      elements.push({
        name: 'revertToState',
        type: FormModal.INPUT_TYPES.SELECT,
        dependsOn: 'durationMillis',
        defaultValue: agent.currentState.state,
        label: 'Revert To',
        options: _.map(Utils.MACHINE_STATES_FOR_REVERT, (machineState) => ({
          label: machineState,
          value: machineState
        }))
      });
    }
    if (buttonType == 'DECOMMISSION') {
      elements.push({
        name: 'killTasksOnDecommissionTimeout',
        type: FormModal.INPUT_TYPES.BOOLEAN,
        dependsOn: 'durationMillis',
        label: 'Kill remaining tasks on decommission timeout',
        defaultValue: false
      });
    }

    return elements;
  };

  const showUser = (agent) => Utils.isIn(agent.currentState.state, ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']);

  const resources = _.uniq(_.flatten(_.map(props.agents, (agent) => Object.keys(Utils.maybe(agent, ['resources'], [])))));

  const customAttrs = _.uniq(_.flatten(_.map(props.agents, (agent) => Object.keys(Utils.maybe(agent, ['attributes'], [])))));

  const expiringAgentState = (agent) => (
    _.find(props.expiringAgentStates, (expiring) => expiring.machineId == agent.id)
  );

  const getMaybeDeactivateHostButton = (agent) => (
    ! Utils.isIn(agent.host, props.inactiveHosts) && (
      <FormModalButton
        name="Mark Inactive"
        buttonChildren={<Glyphicon glyph="remove-circle" />}
        action="Mark Host Inactive"
        onConfirm={() => props.deactivateHost(agent.host)}
        tooltipText={`Flag host '${agent.host}' as inactive`}
        formElements={[]}
      >
        <p>Are you sure you want to mark the host {agent.host} as inactive?</p>
        <p>
          This will automatically decommission any agent that joins with this hostname.
        </p>
      </FormModalButton>
    )
  );

  const getMaybeReactivateHostButton = (agent) => (
    Utils.isIn(agent.host, props.inactiveHosts) && (
      <FormModalButton
        name="Reactivate Host"
        buttonChildren={<Glyphicon glyph="ok-circle" />}
        action="Mark Host Active"
        onConfirm={() => props.reactivateHost(agent.host)}
        tooltipText={`Mark host '${agent.host}' as active`}
        formElements={[]}
      >
        <p>Are you sure you want to reactivate host {agent.host}?</p>
        <p>
          New agents from this host will no longer automatically be marked as
          decommissioned.
        </p>
      </FormModalButton>
    )
  );

  const clearInactiveHostsButton = (
      <FormModalButton
        name="Clear Inactive Hosts"
        buttonChildren={<Glyphicon glyph="remove-circle" />}
        action="Clear Inactive Hosts"
        onConfirm={() => props.clearInactiveHosts()}
        tooltipText={`Clear inactive hosts list`}
        formElements={[]}
      >
        <p>Are you sure you want to clear all inactive hosts?</p>
      </FormModalButton>
    );

  const clearInactiveAgentsButton = (
      <FormModalButton
        name="Clear Inactive Agents"
        buttonChildren={<Glyphicon glyph="remove-circle" />}
        action="Clear Inactive Agents"
        onConfirm={() => props.clearInactiveAgents()}
        tooltipText={`Clear inactive agents list`}
        formElements={[]}
      >
        <p>Are you sure you want to clear all dead agents?</p>
      </FormModalButton>
    );

  const getMaybeReactivateButton = (agent) => (
    Utils.isIn(agent.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']) && (
      <FormModalButton
        name="Reactivate Agent"
        buttonChildren={<Glyphicon glyph="new-window" />}
        action="Reactivate Agent"
        onConfirm={(data) => props.reactivateAgent(agent, data)}
        tooltipText={`Reactivate ${agent.id}`}
        formElements={actionElements(agent, 'REACTIVATE')}>
        <p>Are you sure you want to cancel decommission and reactivate this agent?</p>
        <pre>{agent.id}</pre>
        <p>Reactivating an agent will cancel the decommission without erasing the agents's history and move it back to the active state.</p>
      </FormModalButton>
  ));

  const getMaybeFreezeButton = (agent) => (agent.currentState.state === 'ACTIVE' &&
    <FormModalButton
      name="Freeze Agent"
      buttonChildren={<Glyphicon glyph="stop" />}
      action="Freeze Agent"
      onConfirm={(data) => props.freezeAgent(agent, data)}
      tooltipText={`Freeze ${agent.id}`}
      formElements={actionElements(agent, 'FREEZE')}>
      <p>Are you sure you want to freeze this agent?</p>
      <pre>{agent.id}</pre>
      <p>Freezing an agent will prevent new tasks from being launched. Previously running tasks will be unaffected.</p>
    </FormModalButton>
  );

  const getMaybeDecommissionButton = (agent) => (Utils.isIn(agent.currentState.state, ['ACTIVE', 'FROZEN']) && (
    <FormModalButton
      name="Decommission Agent"
      buttonChildren={<Glyphicon glyph="trash" />}
      action="Decommission Agent"
      onConfirm={(data) => props.decommissionAgent(agent, data)}
      tooltipText={`Decommission ${agent.id}`}
      formElements={actionElements(agent, 'DECOMMISSION')}>
      <p>Are you sure you want to decommission this agent?</p>
      <pre>{agent.id}</pre>
      <p>Decommissioning an agent causes all tasks currently running on it to be rescheduled and executed elsewhere,
      as new tasks will no longer consider the agent with id <code>{agent.id}</code> a valid target for execution.
      This process may take time as replacement tasks must be considered healthy before old tasks are killed.</p>
    </FormModalButton>
  ));

  const getMaybeRemoveButton = (agent) => (!Utils.isIn(agent.currentState.state, ['ACTIVE', 'FROZEN']) && (
    <FormModalButton
      name="Remove Agent"
      buttonChildren={<Glyphicon glyph="remove" />}
      action="Remove Agent"
      onConfirm={(data) => props.removeAgent(agent, data)}
      tooltipText={`Remove ${agent.id}`}
      formElements={actionElements(agent, 'REMOVE')}>
      <p>Are you sure you want to remove this agent?</p>
      <pre>{agent.id}</pre>
      {Utils.isIn(agent.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']) &&
      <p>
        Removing a decommissioned agent will cause that agent to become active again if the mesos-agent process is still running.
      </p>}
    </FormModalButton>
  ));

  const getMaybeRemoveExpiring = (agent) => ( expiringAgentState(agent) && (
    <FormModalButton
      name="Cancel Expiring ACtion"
      buttonChildren={<Glyphicon glyph="remove-circle" />}
      action={`Make ${agent.currentState.state} Permanent`}
      onConfirm={(data) => props.removeExpiringState(agent.id)}
      tooltipText={`Cancel revert to ${expiringAgentState(agent).revertToState}`}
      formElements={[]}>
      <p>Are you sure you want to remove the expiring action for this agent? This will make the curretn state permanent.</p>
      <pre>{agent.id}</pre>
    </FormModalButton>
  ));

  const idColumn = () => (
    <Column
      label="ID"
      id="id"
      key="id"
      sortable={true}
      sortData={(cellData, agent) => agent.id}
      cellData={(agent) => (
        <Link to={`tasks/active/all/${agent.host}`} title={`All tasks running on host ${agent.host}`}>
          {agent.id}
        </Link>
      )}
    />
  );

  const stateColumn = () => (
    <Column
      label="State"
      id="state"
      key="state"
      sortable={true}
      sortData={(cellData, agent) => agent.currentState.state}
      cellData={(agent) => Utils.humanizeText(agent.currentState.state)}
    />
  );

  const sinceColumn = () => (
    <Column
      label="Since"
      id="timestamp"
      key="timestamp"
      sortable={true}
      sortData={(cellData, agent) => agent.currentState.timestamp}
      cellData={(agent) => Utils.absoluteTimestamp(agent.currentState.timestamp)}
    />
  );

  const rackColumn = () => (
    <Column
      label="Rack"
      id="rack"
      key="rack"
      sortable={true}
      sortData={(cellData, agent) => agent.rackId}
      cellData={(agent) => agent.rackId}
    />
  );

  const hostColumn = () => (
    <Column
      label="Host"
      id="host"
      key="host"
      sortable={true}
      sortData={(cellData, agent) => agent.host}
      cellData={(agent) => agent.host}
    />
  );

  const uptimeColumn = () => (
    <Column
      label="Uptime"
      id="uptime"
      key="uptime"
      sortable={true}
      sortData={(cellData, agent) => agent.firstSeenAt}
      cellData={(agent) => Utils.duration(Date.now() - agent.firstSeenAt)}
    />
  );

  const messageColumn = () => (
    <Column
      label="Message"
      id="message"
      key="message"
      cellData={(agent) => agent.currentState.message}
    />
  );

  const expiringColumn = () => (
    <Column
      label="Expiring"
      id="expiring"
      key="expiring"
      cellData={(agent) => {
        const expiring = expiringAgentState(agent);
        if (expiring) {
          return `Transitions to ${expiring.revertToState} in ${Utils.duration(Date.now() - (expiring.startMillis + expiring.expiringAPIRequestObject.durationMillis))}`
        }
      }}
    />
  );

  const getColumns = (type) => {
    const columns = [];
    if (props.columnSettings['id']) {
      columns.push(idColumn());
    }
    if (props.columnSettings['state']) {
      columns.push(stateColumn());
    }
    if (props.columnSettings['since']) {
      columns.push(sinceColumn());
    }
    if (props.columnSettings['rack']) {
      columns.push(rackColumn());
    }
    if (props.columnSettings['host']) {
      columns.push(hostColumn());
    }
    if (props.columnSettings['uptime']) {
      columns.push(uptimeColumn());
    }

    if (typeName[type] && props.columnSettings['actionUser']) {
      columns.push(
        <Column
          label={typeName[type]}
          id="typename"
          key="typename"
          sortable={true}
          sortData={(cellData, agent) => agent.currentState.user || ''}
          cellData={(agent) => showUser(agent) && agent.currentState.user}
        />
      );
    }
    if (props.columnSettings['message']) {
      columns.push(messageColumn());
    }
    if (!_.isEmpty(props.expiringAgentStates) && props.columnSettings['expiring']) {
      columns.push(expiringColumn());
    }

    _.each(resources, (resource) => {
        if (props.columnSettings[resource]) {
          columns.push(
            <Column
              label={resource}
              id={resource}
              key={resource}
              sortable={true}
              cellData={(agent) => Utils.maybe(agent, ['resources', resource], 0)}
            />
          );
        }
      }
    );

    _.each(customAttrs, (attr) => {
        if (props.columnSettings[attr]) {
          columns.push(
            <Column
              label={attr}
              id={attr}
              key={attr}
              sortable={true}
              cellData={(agent) => Utils.maybe(agent, ['attributes', attr], '')}
            />
          );
        }
      }
    );

    columns.push(
      <Column
        id="actions-column"
        key="actions-column"
        className="actions-column"
        cellData={(agent) => (
          <span>
            {getMaybeDeactivateHostButton(agent)}
            {getMaybeReactivateHostButton(agent)}
            {getMaybeRemoveExpiring(agent)}
            {getMaybeReactivateButton(agent)}
            {getMaybeFreezeButton(agent)}
            {getMaybeDecommissionButton(agent)}
            {getMaybeRemoveButton(agent)}
            <JSONButton object={agent} showOverlay={true}>
              {'{ }'}
            </JSONButton>
          </span>
        )}
      />
    );

    return columns;
  };

  const activeAgents = props.agents.filter(({currentState}) => currentState.state === 'ACTIVE');

  const frozenAgents = props.agents.filter(({currentState}) => currentState.state === 'FROZEN');

  const decommissioningAgents = props.agents.filter(({currentState}) => Utils.isIn(currentState.state, ['DECOMMISSIONING', 'STARTING_DECOMMISSION']));

  const decommissionedAgents = props.agents.filter(({currentState}) => currentState.state === 'DECOMMISSIONED');

  const inactiveAgents = props.agents.filter(({currentState}) => Utils.isIn(currentState.state, ['DEAD', 'MISSING_ON_STARTUP']));

  const inactiveHostsPanel = (inactiveHosts) => (
    inactiveHosts && inactiveHosts.length > 0 && (
      <div className="row">
        <h3>Inactive Hosts
            <span className="pull-right">
              {clearInactiveHostsButton}
            </span>
        </h3>
        <p>These hosts are marked as inactive: </p>
        <ul className="list-group">
          {inactiveHosts.map((host) => (
            <li className="list-group-item" key={host}>
              {host}
              <span className="pull-right">
                {getMaybeReactivateHostButton({host})}
              </span>
            </li>
          ))}
        </ul>
      </div>
    )
  );

  const states = [
    {
      stateName: 'Active',
      emptyMessage: 'No Active Agents',
      hostsInState: activeAgents,
      columns: getColumns('active'),
      paginated: props.paginated
    },
    {
      stateName: 'Frozen',
      emptyMessage: 'No Frozen Agents',
      hostsInState: frozenAgents,
      columns: getColumns('decommissioning'),
      paginated: props.paginated
    },
    {
      stateName: 'Decommissioning',
      emptyMessage: 'No Decommissioning Agents',
      hostsInState: decommissioningAgents,
      columns: getColumns('decommissioning'),
      paginated: props.paginated
    },
    {
      stateName: 'Decommissioned',
      emptyMessage: 'No Decommissioned Agents',
      hostsInState: decommissionedAgents,
      columns: getColumns('decommissioned'),
      paginated: props.paginated
    },
    {
      stateName: 'Inactive',
      emptyMessage: 'No Inactive Agents',
      hostsInState: inactiveAgents,
      columns: getColumns('inactive'),
      paginated: props.paginated,
      clearAllButton: clearInactiveAgentsButton
    }
  ];

  return (
    <div>
      <CustomizeAgentsTableButton
        columns={props.columnSettings}
        paginated={props.paginated}
        availableAttributes={customAttrs}
        availableResources={resources}
      >
        <button
          className="btn btn-primary pull-right"
          alt="Customize Columns"
          title="Customize">
          Customize
        </button>
      </CustomizeAgentsTableButton>
      <MachinesPage
        header = "Agents"
        states = {states}
        error = {props.error}
      />
    {inactiveHostsPanel(props.inactiveHosts)}
    </div>
  );
};

Agents.propTypes = {
  freezeAgent: PropTypes.func.isRequired,
  decommissionAgent: PropTypes.func.isRequired,
  removeAgent: PropTypes.func.isRequired,
  reactivateAgent: PropTypes.func.isRequired,
  fetchExpiringAgentStates: PropTypes.func.isRequired,
  removeExpiringState: PropTypes.func.isRequired,
  clear: PropTypes.func.isRequired,
  error: PropTypes.string,
  expiringAgentStates: PropTypes.array.isRequired,
  agents: PropTypes.arrayOf(PropTypes.shape({
    state: PropTypes.string
  })),
  inactiveHosts: PropTypes.arrayOf(PropTypes.string),
  columnSettings: PropTypes.object.isRequired,
  paginated: PropTypes.bool.isRequired,
  clearInactiveHosts: PropTypes.func.isRequired,
  clearInactiveAgents: PropTypes.func.isRequired
};

function getErrorFromState(state) {
  const { freezeAgent, decommissionAgent, removeAgent, reactivateAgent } = state.api;
  if (freezeAgent.error) {
    return `Error freezing agent: ${ state.api.freezeAgent.error.message }`;
  }
  if (decommissionAgent.error) {
    return `Error decommissioning agent: ${ state.api.decommissionAgent.error.message }`;
  }
  if (removeAgent.error) {
    return `Error removing agent: ${ state.api.removeAgent.error.message }`;
  }
  if (reactivateAgent.error) {
    return `Error reactivating agent: ${ state.api.reactivateAgent.error.message }`;
  }
  return null;
}

function mapStateToProps(state) {
  return {
    inactiveHosts: state.api.inactiveHosts.data,
    agents: state.api.agents.data,
    error: getErrorFromState(state),
    columnSettings: state.ui.agents.columns,
    paginated: state.ui.agents.paginated,
    expiringAgentStates: state.api.expiringAgentStates.data
  };
}

function mapDispatchToProps(dispatch) {
  function clear() {
    return Promise.all([
      dispatch(FreezeAgent.clear()),
      dispatch(DecommissionAgent.clear()),
      dispatch(RemoveAgent.clear()),
      dispatch(ReactivateAgent.clear())
    ]);
  }
  function fetchAgentsAndExpiring() {
    return Promise.all([
      dispatch(FetchAgents.trigger()),
      dispatch(FetchExpiringAgentStates.trigger())
    ]);
  }
  return {
    fetchAgents: () => dispatch(FetchAgents.trigger()),
    freezeAgent: (agent, message) => { clear().then(() => dispatch(FreezeAgent.trigger(agent.id, message)).then(() => fetchAgentsAndExpiring())); },
    decommissionAgent: (agent, message) => { clear().then(() => dispatch(DecommissionAgent.trigger(agent.id, message)).then(() => fetchAgentsAndExpiring())); },
    removeAgent: (agent, message) => { clear().then(() => dispatch(RemoveAgent.trigger(agent.id, message)).then(() => fetchAgentsAndExpiring())); },
    deactivateHost: (host) =>
      clear()
        .then(() => dispatch(DeactivateHost.trigger(host)))
        .then(() => Promise.all([
          fetchAgentsAndExpiring(),
          dispatch(FetchInactiveHosts.trigger()),
        ])),
    reactivateHost: (host) =>
      clear()
        .then(() => dispatch(ReactivateHost.trigger(host)))
        .then(() => Promise.all([
          fetchAgentsAndExpiring(),
          dispatch(FetchInactiveHosts.trigger()),
        ])),
    fetchInactiveHosts: () => dispatch(FetchInactiveHosts.trigger()),
    clearInactiveHosts: () => dispatch(ClearInactiveHosts.trigger()).then(() => fetchAgentsAndExpiring()),
    clearInactiveAgents: () => dispatch(ClearInactiveAgents.trigger()).then(() => dispatch(FetchInactiveHosts.trigger())),
    reactivateAgent: (agent, message) => { clear().then(() => dispatch(ReactivateAgent.trigger(agent.id, message)).then(() => fetchAgentsAndExpiring())); },
    fetchExpiringAgentStates: () => dispatch(FetchExpiringAgentStates.trigger()),
    removeExpiringState: (agentId) => { clear().then(() => dispatch(RemoveExpiringAgentState.trigger(agentId)).then(() => fetchAgentsAndExpiring())); },
    clear
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(Agents, refresh, true, true, initialize));
