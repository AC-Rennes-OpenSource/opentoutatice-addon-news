<html>
    <body>
        <#if news.display >
        
            <h3>Nouveautés de votre espace ${spaceTitle}</h3>
            
            <p>${news.newMembersCount} personnes ont rejoint votre espace.</p>
            
            <ul style="list-style-type: none">
                <#list news.docs as docItem >
                    <li><a href="${docItem.link}">${docItem.title}</a>, ${docItem.evtBegin}par ${docItem.lastContributor}</li>
                </#list>
            </ul>
            
            <#if news.otherDocsCount gt 0 >
            ... et ${news.otherDocsCount} autres actualités depuis le ${lastSendDate}.
            </#if>
            
       </#if>
       
       <#if activities.display >
        
         <h3>Dernières contributions</h3>
        
         <ul style="list-style-type: none">
            <#list activities.docs as docItem >
                <li><a href="${docItem.link}">${docItem.title}</a> (${docItem.doc.type}), le ${docItem.modified} par ${docItem.lastContributor}</li>
            </#list>
         </ul>
        
         <#if activities.otherDocsCount gt 0 >
         ... et ${activities.otherDocsCount} autres contributions depuis le ${lastSendDate}.
         </#if>
        
      </#if>
        
     <#if !news.display && !activities.display >
     Aucune activité ou contribution depuis le ${lastSendDate}.
     </#if>
     
    </body>
</html>