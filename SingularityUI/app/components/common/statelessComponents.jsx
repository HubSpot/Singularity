import React, { PropTypes } from 'react';
import { Panel, ProgressBar } from 'react-bootstrap';
import CopyToClipboard from 'react-copy-to-clipboard';

export const DeployState = (props) => {
  return (
    <span className="deploy-state" data-state={props.state || 'PENDING'}>
      {props.state}
    </span>
  );
};

DeployState.propTypes = {
  state: PropTypes.string
};

export const InfoBox = (props) => {
  let { value } = props;
  if (value instanceof Array) {
    value = value.join(props.join);
  }
  return (
    <li className="col-sm-6 col-md-3">
      <div>
        <h4>
          {props.name}
          <CopyToClipboard text={value}>
            <a className="copy-btn">Copy</a>
          </CopyToClipboard>
        </h4>
        <p>{value}</p>
      </div>
    </li>
  );
};

InfoBox.propTypes = {
  name: PropTypes.string,
  join: PropTypes.string,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
    PropTypes.bool,
    PropTypes.arrayOf(PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
      PropTypes.bool
    ]))
  ]).isRequired
};

export const UsageInfo = (props) => {
  return (
    <Panel header={props.title}>
      <ProgressBar active={true} bsStyle={props.style} max={props.total} now={props.used} />
      <span>{props.children}</span>
    </Panel>
  );
};

UsageInfo.propTypes = {
  title: PropTypes.string,
  style: PropTypes.string,
  total: PropTypes.number,
  used: PropTypes.number,
  children: PropTypes.node
};
