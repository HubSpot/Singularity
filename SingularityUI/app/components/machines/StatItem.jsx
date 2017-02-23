import React, { PropTypes } from 'react';
import { Link } from 'react-router';
import CopyToClipboard from 'react-copy-to-clipboard';

const valueWithPotentialLink = (value, maybeLink) => {
  if (maybeLink) {
    return <Link to={maybeLink.href} title={maybeLink.title}>{value}</Link>;
  } else {
    return value;
  }
};

const StatItem = ({name, value, maybeLink, percentage, className}) => {
  return (
    <CopyToClipboard text={value.toString()}>
      <li className={`${className} stat-item-detail container`}>
          <div className="row">
            <div className="col-xs-3" id="stat-name">
              {name}
            </div>
            <div className="col-xs-6" id="stat-value">
              {valueWithPotentialLink(value, maybeLink)}
            </div>
            {percentage &&
              <div className="col-xs-3" id="stat-percentage">
                ({percentage}%)
              </div>
            }
          </div>
      </li>
    </CopyToClipboard>
  );
};

StatItem.propTypes = {
  name : PropTypes.string,
  value : PropTypes.string,
  maybeLink : PropTypes.shape({
    href : PropTypes.string,
    title : PropTypes.string
  }),
  percentage : PropTypes.number,
  className : PropTypes.string
};

export default StatItem;
