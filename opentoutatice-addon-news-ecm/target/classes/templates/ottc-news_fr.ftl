<html>
    <body>
    
        <#if news.display >
        
            <h3>Nouveautés de votre espace ${spaceTitle}</h3>
            
            <#if news.newMembersCount gt 0>
                <#if news.newMembersCount == 1>
                    <p>${news.newMembersCount} personne a rejoint votre espace.</p>
                <#else>
                    <p>${news.newMembersCount} personnes ont rejoint votre espace.</p>
                </#if>
            </#if>
            
            <ul style="list-style-type: none; margin-left:0; padding-left: 0;">
                <#list news.docs as docItem >
                    <li style="margin-left:0;"><a href="${docItem.link}" style="text-decoration: none;">${docItem.title}</a>, ${docItem.evtBegin}par ${docItem.lastContributor}</li>
                </#list>
            </ul>
            
            <#if news.otherDocsCount gt 0 >
                <#if news.otherDocsCount gt 1 >
                ... et ${news.otherDocsCount} autres actualités depuis le ${lastSendDate}.
                <#else>
                ... et ${news.otherDocsCount} autre actualité depuis le ${lastSendDate}.
                </#if>
            </#if>
            
       </#if>
       
       <#if activities.display >
        
           <#if news.display>
                <h3>Dernières contributions</h3>
           <#else>
                <h3>Dernières contributions de votre espace ${spaceTitle}</h3>
           </#if>
        
         <ul style="list-style-type: none; margin-left:0; padding-left: 0;">
            <#list activities.docs as docItem >
                <li style="margin-left:0;"><a href="${docItem.link}" style="text-decoration: none;">${docItem.title}</a>, le ${docItem.modified} par ${docItem.lastContributor}</li>
            </#list>
         </ul>
        
         <#if activities.otherDocsCount gt 0 >
            <#if activities.otherDocsCount gt 1 >
                ... et ${activities.otherDocsCount} autres contributions depuis le ${lastSendDate}.
                <#else>
                ... et ${activities.otherDocsCount} autre contribution depuis le ${lastSendDate}.
                </#if>
         </#if>
        
      </#if>
     
    </body>
</html>