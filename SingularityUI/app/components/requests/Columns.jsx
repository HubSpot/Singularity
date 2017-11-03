import React from 'react';
import { Link } from 'react-router';

import Column from '../common/table/Column';

import Utils from '../../utils';

import JSONButton from '../common/JSONButton';
import { Glyphicon, Dropdown, MenuItem } from 'react-bootstrap'
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import RequestStar from './RequestStar';
import UnpauseButton from '../common/modalButtons/UnpauseButton';
import PauseButton from '../common/modalButtons/PauseButton';
import RemoveButton from '../common/modalButtons/RemoveButton';
import RunNowButton from '../common/modalButtons/RunNowButton';
import ScaleButton from '../common/modalButtons/ScaleButton';

export const Starred = (
  <Column
    label=""
    id="starred"
    key="starred"
    cellData={
      (rowData) => rowData.request.id
    }
    cellRender={
      (cellData) => (
        <RequestStar requestId={cellData} />
      )
    }
  />
);

export const DeployUser = (
  <Column
    label="Deploy User"
    id="user"
    key="user"
    cellData={
      (rowData) => {
        const activeDeployUser = Utils.maybe(rowData, [
          'requestDeployState',
          'activeDeploy',
          'user'
        ]);

        if (activeDeployUser !== undefined) {
          // assume user is an email address
          return activeDeployUser.split('@')[0];
        }

        return '';
      }
    }
    sortable={true}
  />
);

export const LastDeploy = (
  <Column
    label="Time of Last Deploy"
    id="lastDeploy"
    key="lastDeploy"
    cellData={
      (rowData) => {
        const activeDeployTimestamp = Utils.maybe(rowData, [
          'requestDeployState',
          'activeDeploy',
          'timestamp'
        ], null);

        return activeDeployTimestamp;
      }
    }
    sortData={(cellData) => cellData || 0}
    cellRender={
      (cellData) => {
        if (cellData) {
          return Utils.timestampFromNow(cellData);
        }
        return '';
      }
    }
    sortable={true}
  />
);

export const RequestId = (
  <Column
    label="Request"
    id="requestId"
    key="requestId"
    className="keep-in-check"
    cellData={
      (rowData) => rowData.id
    }
    sortData={(cellData) => cellData.toLowerCase()}
    cellRender={
      (cellData) => (
        <Link to={`request/${cellData}`}>
          {cellData}
        </Link>
      )
    }
    sortable={true}
  />
);

export const State = (
  <Column
    label="Status"
    id="state"
    key="state"
    cellData={
      (rowData) => Utils.humanizeText(rowData.state) //PAUSED DELETING ACTIVE SYSTEM_COOLDOWN
    }
    cellRender={(cellData, rowData) => {
        const tooltip = (
          <ToolTip id="view-request-state">
            {cellData}
          </ToolTip>
        )
        return (
          <OverlayTrigger placement="top" id="view-request-state-overlay" overlay={tooltip}>
            <Glyphicon glyph={Utils.glyphiconForRequestState(rowData.state)} />
          </OverlayTrigger>
        );
      }
    }
    sortable={true}
  />
);

export const Type = (
  <Column
    label="Type"
    id="type"
    key="type"
    cellData={
      (rowData) => Utils.humanizeText(rowData.request.requestType)
    }
    cellRender={(cellData, rowData) => {
        const tooltip = (
          <ToolTip id="view-request-type">
            {cellData}
          </ToolTip>
        )
        return (
          <OverlayTrigger placement="top" id="view-request-type-overlay" overlay={tooltip}>
            <Glyphicon glyph={Utils.glyphiconForType(rowData.request.requestType)} />
          </OverlayTrigger>
        );
      }
    }
    sortable={true}
  />
);

export const Instances = (
  <Column
    label="Instances"
    id="instances"
    key="instances"
    cellData={
      (rowData) => rowData.request.instances
    }
    sortData={(cellData) => cellData || 0}
    sortable={true}
  />
);

export const DeployId = (
  <Column
    label="Deploy ID"
    id="deployId"
    key="deployId"
    cellData={
      (rowData) => rowData.requestDeployState && rowData.requestDeployState.activeDeploy
    }
    sortData={(cellData) => (cellData ? cellData.deployId : '')}
    cellRender={(cellData) => {
      if (cellData) {
        return (
          <Link to={`request/${cellData.requestId}/deploy/${cellData.deployId}`}>
            {cellData.deployId}
          </Link>
        );
      }
      return undefined;
    }}
    sortable={true}
  />
);

export const Group = (
  <Column
    label="Group"
    id="group"
    key="group"
    cellData={
      (rowData) => rowData.request.group
    }
    sortData={(cellData) => cellData || ''}
    sortable={true}
  />
);

const editTooltip = (
  <ToolTip id="edit">
    Edit Request
  </ToolTip>
);

export const Actions = (
  <Column
    label=""
    id="actions"
    key="actions"
    className="actions-column"
    cellRender={
      (cellData, rowData) => {
        const edit = !config.hideNewRequestButton && (
          <li className="col-xs-2">
            <OverlayTrigger placement="top" id="view-edit-overlay" overlay={editTooltip}>
              <Link to={`requests/edit/${rowData.id}`} alt="Edit">
                <span className="glyphicon glyphicon-edit"></span>
              </Link>
            </OverlayTrigger>
          </li>
        );

        const unpause = cellData.state === 'PAUSED' && (
          <li className="col-xs-2"><UnpauseButton requestId={cellData.id} /></li>
        );

        const pause = cellData.state != 'PAUSED' && (
          <li className="col-xs-2">
            <PauseButton
              requestId={cellData.id}
              isScheduled={cellData.requestType === 'SCHEDULED'}
            />
          </li>
        );

        const scale = cellData.canBeScaled && (
          <li className="col-xs-2">
            <ScaleButton
              requestId={cellData.id}
              currentInstances={cellData.request.instances}
              bounceAfterScaleDefault={Utils.maybe(cellData.request, ['bounceAfterScale'], false)}
            />
          </li>
        );

        const runNow = cellData.canBeRunNow && (
          <li className="col-xs-2"><RunNowButton requestId={cellData.id} /></li>
        );

        return (
          <Dropdown id={rowData.id} pullRight>
            <Dropdown.Toggle>
              Actions
            </Dropdown.Toggle>
            <Dropdown.Menu>
              {scale}
              {runNow}
              {unpause}
              {pause}
              <li className="col-xs-2">
                <RemoveButton 
                  requestId={cellData.id}
                  loadBalancerData={Utils.maybe(cellData, ['activeDeploy', 'loadBalancerOptions'], {})}
                />
              </li>
              <li className="col-xs-2">
                <JSONButton className="inline" object={cellData} showOverlay={true}>
                  {'{ }'}
                </JSONButton>
              </li>
              {edit}
            </Dropdown.Menu>
          </Dropdown>
        );
      }
    }
  />
);

export const PendingType = (
  <Column
    label="Pending Type"
    id="schedule"
    key="schedule"
    sortable={true}
    cellData={
      (rowData) => rowData.pendingType
    }
    cellRender={(cellData) => Utils.humanizeText(cellData)}
  />
);

export const CleaningUser = (
  <Column
    label="User"
    id="user"
    key="user"
    sortable={true}
    cellData={
      (rowData) => rowData.user
    }
    cellRender={(cellData) => cellData.split('@')[0]}
  />
);

export const CleaningTimestamp = (
  <Column
    label="Timestamp"
    id="timestamp"
    key="timestamp"
    sortable={true}
    cellData={
      (rowData) => rowData.timestamp
    }
    cellRender={(cellData) => Utils.timestampFromNow(cellData)}
  />
);

export const CleanupType = (
  <Column
    label="Cleaning Type"
    id="cleanupType"
    key="cleanupType"
    sortable={true}
    cellData={
      (rowData) => rowData.cleanupType
    }
    cellRender={(cellData) => Utils.humanizeText(cellData)}
  />
);
