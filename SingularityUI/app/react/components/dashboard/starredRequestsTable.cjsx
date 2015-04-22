#  

starredRequestsTable = React.createClass


  getDefaultProps: ->
    starredRequests: []

  render: ->
    return (<div></div>)
    # tbody = @props.starredRequests.map (item) =>
    #   console.log item
    #   link = "#{config.appRoot}/request/#{item.id}"
    #   return(
    #     <tr data-request-id="{ item.id }">
    #         <td>
    #             <a className="star" data-action="unstar" data-starred="true">
    #                 <span className="glyphicon glyphicon-star"></span>
    #             </a>
    #         </td>
    #         <td>
    #             <a href={link}>
    #                 { item.id }
    #             </a>
    #         </td>
    #         <td className="hidden-xs" data-value="">
    #             <span title=>
                    
    #             </span>
    #         </td>
    #         <td className="visible-lg visible-xl">
                
    #         </td>
    #         <td className="visible-lg visible-xl">
    #             { item.instances }
    #         </td>
    #     </tr>
    #   )

    # return (
    #   <table className="table table-striped">
    #     <thead>
    #       <tr>
    #         <th data-sortable="false"></th>
    #         <th data-sort-attribute="request.id">Request</th>
    #         <th className="hidden-xs" data-sort-attribute="">Requested</th>
    #         <th className="visible-lg visible-xl" data-sort-attribute="">Deploy user</th>
    #         <th className="visible-lg visible-xl" data-sort-attribute="">Instances</th>
    #       </tr>
    #     </thead>
    #     <tbody>
    #       {tbody}
    #     </tbody>
    #   </table>
    # )

module.exports = starredRequestsTable