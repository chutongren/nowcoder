$(function(){
	$("#publishBtn").click(publish);// 点击id为#publishBtn的按钮，触发publish函数
});

function publish() {
	$("#publishModal").modal("hide"); // 隐藏id为publishModal的发布帖子窗口

	// 获取标题和内容
	var title = $("#recipient-name").val(); //获取文本框内容
	var content = $("#message-text").val();
	// 发送异步请求(POST)
	$.post(
		CONTEXT_PATH + "/discuss/add",
		{"title":title,"content":content},
		function(data) {
			data = $.parseJSON(data);
			// 在提示框中显示返回消息
			$("#hintBody").text(data.msg);
			// 显示提示框
			$("#hintModal").modal("show");
			// 2秒后,自动隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				// 刷新页面
				if(data.code == 0) {
					window.location.reload();
				}
			}, 2000);
		}
	);
}