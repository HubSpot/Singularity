import React from 'react';

export const DeployState = (props) => {
  return (
    <span className="deploy-state" data-state={props.state || 'PENDING'}>
        {props.state}
    </span>
  );
};

export const InfoBox = (props) => {
  return (
    <li className="col-sm-6 col-md-3">
        <div>
            <h4>{props.name}<a className={props.copyableClassName} data-clipboard-text={props.value}>Copy</a></h4>
            <p>{props.value}</p>
        </div>
    </li>
  );
};
