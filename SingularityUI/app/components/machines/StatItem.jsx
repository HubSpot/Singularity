import React, { PropTypes } from 'react';
import CopyToClipboard from 'react-copy-to-clipboard';

const StatItem = ({name, value, percentage, className}) => {
  return (
    <CopyToClipboard text={value.toString()}>
      <li className={`${className} stat-item-detail container`}>
          <div className="row">
            <div className="col-xs-3" id="stat-name">
              {name}
            </div>
            <div className="col-xs-6" id="stat-value">
              {value}
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
  percentage : PropTypes.number,
  className : PropTypes.string
};

export default StatItem;
