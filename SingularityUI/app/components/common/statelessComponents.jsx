import React from 'react';
import { Panel, ProgressBar } from 'react-bootstrap';

export const DeployState = (props) => {
  return (
    <span className="deploy-state" data-state={props.state || 'PENDING'}>
        {props.state}
    </span>
  );
};

export const InfoBox = (props) => {
  return (
    <li className={`col-sm-6 col-md-3`}>
        <div>
            <h4>{props.name}<a className={props.copyableClassName} data-clipboard-text={props.value}>Copy</a></h4>
            <p>{props.value}</p>
        </div>
    </li>
  );
};

export const UsageInfo = (props) => {
  return (
    <Panel header={props.title}>
      <ProgressBar active bsStyle={props.style} max={props.total} now={props.used} />
      <span>{props.text}</span>
    </Panel>
  );
}
