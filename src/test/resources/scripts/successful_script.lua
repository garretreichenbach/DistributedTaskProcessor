function test_success(env)
    local result = {}
    result.message = 'Hello from Lua!'
    result.task_id = "test-id"
    result.computed = 42
    return result
end

return test_success(env)