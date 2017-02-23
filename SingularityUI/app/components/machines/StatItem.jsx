import React, { PropTypes } from 'react';
import { Link } from 'react-router';
import CopyToClipboard from 'react-copy-to-clipboard';

const valueWithPotentialLink = (value, maybeLink) => {
  if (maybeLink) {
    return <Link to={maybeLink.href} title={maybeLink.title}>{value}</Link>;
  }

  return value;
};

const StatItem = ({name, value, maybeLink, percentage}) => {
  return (
    <li className="stat-item-detail container">
        <div className="row">
          <CopyToClipboard text={name.toString()}>
            <div className="col-xs-3" id="stat-name">
              {name}
            </div>
          </CopyToClipboard>
          <CopyToClipboard text={value.toString()}>
            <div className="col-xs-6" id="stat-value">
              {valueWithPotentialLink(value, maybeLink)}
            </div>
          </CopyToClipboard>
          {percentage != null &&
            <CopyToClipboard text={percentage.toString()}>
              <div className="col-xs-3" id="stat-percentage">
                {percentage}%
              </div>
            </CopyToClipboard>
          }
        </div>
    </li>
  );
};

StatItem.propTypes = {
  name : PropTypes.string,
  value : PropTypes.string,
  maybeLink : PropTypes.shape({
    href : PropTypes.string,
    title : PropTypes.string
  }),
  percentage : PropTypes.number
};

export default StatItem;
