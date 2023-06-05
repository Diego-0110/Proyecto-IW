function togglePicForm(){
    let formCont = document.getElementById('pic-form-cont');
    if(formCont.style.display == 'none'){
        formCont.style.display = 'flex';
    }
    else {
        formCont.style.display = 'none';
        document.getElementById('report-desc').value = '';
    }
}

function reportUser(userTarget, description = ""){
    description = document.getElementById('report-desc').value;
    // let event = document.getElementById('report-event').value;
    let cause = document.getElementById('report-cause').value;
    go(`/user/${userTarget}/report`, "POST", {description, cause}, false)
        .then(d => {console.log(d);
            toggleModalForm('report-form-cont');})
        .catch(e => {console.log(e)
            alert("Something went wrong.");
            });
}