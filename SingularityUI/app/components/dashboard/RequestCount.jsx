import React, { Component, PropTypes } from 'react';

const RequestCount = ({label, count, link}) =>
  <a className='big-number-link' href={link}>
    <div className='well'>
      <div className='big-number'>
        <div className='number' data-state-attribute='requests'>
          {count}
        </div>
        <div className='number-label'>{label}</div>
      </div>
    </div>
  </a>;

export default RequestCount;
