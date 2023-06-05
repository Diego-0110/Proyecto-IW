function reportUser(userTarget, description = ""){
    description = document.getElementById('report-desc').value;
    let event = document.getElementById('report-event').value;
    let cause = document.getElementById('report-cause').value;
    go(`/user/${userTarget}/report`, "POST", {description, event, cause}, false)
        .then(d => {console.log(d);
            toggleReportForm();})
        .catch(e => {console.log(e)
            alert("Something went wrong.");
            });
}