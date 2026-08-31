param(
    [int]$Port = 18110
)

$listener = [System.Net.HttpListener]::new()
$listener.Prefixes.Add("http://127.0.0.1:$Port/")
$listener.Start()
try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response
        $response.ContentType = "application/json; charset=utf-8"
        $body = switch -Regex ($request.Url.AbsolutePath) {
            '^/lessons/[0-9]+/valid$' { '1'; break }
            '^/lessons/now$' { '{"courseId":9,"courseName":"Agent Test Course"}'; break }
            '^/lessons/page$' { '[{"courseId":9,"courseName":"Agent Test Course"}]'; break }
            '^/learning-records/course/[0-9]+$' { '{"courseId":9,"progress":50}'; break }
            '^/courses/[0-9]+/catalogs$' { '[{"id":11,"name":"Agent Test Section"}]'; break }
            '^/questions/listOfBiz$' { '[{"id":1,"name":"Test question","answer":"secret","analysis":"secret"}]'; break }
            '^/courses/portal$' { '[{"id":9,"name":"Agent Test Course"}]'; break }
            default { '{}' }
        }
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
        $response.StatusCode = 200
        $response.ContentLength64 = $bytes.Length
        $response.OutputStream.Write($bytes, 0, $bytes.Length)
        $response.Close()
    }
}
finally {
    $listener.Stop()
    $listener.Close()
}
