import React, { PropTypes } from 'react';
import { STAT_STYLES } from './Constants';
import CopyToClipboard from 'react-copy-to-clipboard';

const SlaveStat = ({name, value, status}) => {
  return (
    <CopyToClipboard text={value}>
      <li className={STAT_STYLES[status] + ' slave-usage-detail'}>
        {name}
      </li>
    </CopyToClipboard>
  );
};

SlaveStat.propTypes = {
  name : PropTypes.string,
  value : PropTypes.string,
  status : PropTypes.string
};

export default SlaveStat;
